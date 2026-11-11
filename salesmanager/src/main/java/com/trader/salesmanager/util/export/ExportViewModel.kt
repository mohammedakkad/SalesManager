package com.trader.salesmanager.util.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.reports.CustomerRank
import com.trader.salesmanager.ui.reports.DaySalesEntry
import com.trader.salesmanager.ui.reports.PaymentShare
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel نظيف — لا Context هنا على الإطلاق.
 *
 * المسؤولية الوحيدة: تشغيل coroutine التصدير وإدارة الحالة.
 * المشاركة/التنزيل تتم في الـ Screen عبر LocalContext.
 *
 * لماذا؟
 *   - Context في ViewModel → memory leak محتمل
 *   - ViewModel لا يجب أن يعرف كيف يُشارك ملفاً (Intent = Android UI concern)
 *   - الـ Screen هي المكان الصحيح لأي Intent/FileProvider
 */
class ExportViewModel : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()
    private var exportJob: Job? = null
    private var exportGeneration = 0L

    // ── API عام ───────────────────────────────────────────────

    fun exportInvoicePdf(
        transaction: Transaction,
        items: List<InvoiceItem>,
        storeName: String,
        customer: Customer? = null,
        priorDebtBalance: Double? = null,
        currentDebtBalance: Double? = null,
        returnSummary: ReturnSummary = ReturnSummary.NONE,
        cacheDir: File           // ← File بدل Context — لا يحتفظ به ViewModel
    ) = runExport("فاتورة_${transaction.id}.pdf") {
        ExportManager.generateInvoicePdf(
            cacheDir = cacheDir,
            transaction = transaction,
            items = items,
            storeName = storeName,
            customer = customer,
            priorDebtBalance = priorDebtBalance,
            currentDebtBalance = currentDebtBalance,
            returnSummary = returnSummary
        )
    }

    fun exportCustomerStatementPdf(
        customer: Customer,
        transactions: List<Transaction>,
        storeName: String,
        cacheDir: File
    ) = runExport("كشف_${customer.name}.pdf") {
        ExportManager.generateCustomerStatementPdf(cacheDir, customer, transactions, storeName)
    }

    fun exportSalesReportExcel(
        transactions: List<Transaction>,
        periodLabel: String,
        storeName: String,
        dailySales: List<DaySalesEntry>,
        topSpenders: List<CustomerRank>,
        paymentShares: List<PaymentShare>,
        cacheDir: File
    ) = runExport("تقرير_المبيعات.xlsx") {
        ExportManager.generateSalesReportExcel(
            cacheDir, transactions, periodLabel, storeName, dailySales, topSpenders, paymentShares
        )
    }

    fun exportMonthlyReportPdf(
        data: ReportPdfGenerator.MonthlyReportPdfData,
        cacheDir: File
    ) = runExport("تقرير_${data.year}_${data.month + 1}.pdf") {
        ExportManager.generateMonthlyReportPdf(cacheDir, data)
    }

    fun exportInventoryExcel(
        products: List<ProductWithUnits>,
        storeName: String,
        cacheDir: File
    ) = runExport("تقرير_المخزون.xlsx") {
        ExportManager.generateInventoryExcel(cacheDir, products, storeName)
    }

    fun dismissError() = reset()

    fun reset() {
        // Invalidate the result before cancelling work that may still finish an IO write.
        exportGeneration += 1
        exportJob?.cancel()
        exportJob = null
        _state.value = ExportState.Idle
    }

    private fun runExport(fileName: String, block: suspend () -> File) {
        if (_state.value is ExportState.Loading) return
        val generation = ++exportGeneration
        // Claim before launch: successive UI calls must observe an active export.
        _state.value = ExportState.Loading("جارٍ تحضير $fileName...")
        val job = viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { block() }
                if (generation == exportGeneration) {
                    val type = if (fileName.endsWith(".pdf")) ExportType.PDF else ExportType.XLSX
                    _state.value = ExportState.Success(fileName, file.absolutePath, type)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (generation == exportGeneration) {
                    _state.value = ExportState.Error(
                        message = when (e) {
                            is OutOfMemoryError -> "لا توجد ذاكرة كافية"
                            is java.io.IOException -> "خطأ في الكتابة: ${e.message}"
                            else -> "فشل التصدير: ${e.message}"
                        },
                        cause = e
                    )
                }
            }
        }
        exportJob = job
        job.invokeOnCompletion { cause ->
            if (generation == exportGeneration) {
                exportJob = null
                if (cause is CancellationException) _state.value = ExportState.Idle
            }
        }
    }
}
