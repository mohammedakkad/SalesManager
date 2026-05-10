package com.trader.salesmanager.util.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.reports.CustomerRank
import com.trader.salesmanager.ui.reports.DaySalesEntry
import com.trader.salesmanager.ui.reports.PaymentShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

    // ── API عام ───────────────────────────────────────────────

    fun exportInvoicePdf(
        transaction: Transaction,
        items: List<InvoiceItem>,
        storeName: String,
        cacheDir: File           // ← File بدل Context — لا يحتفظ به ViewModel
    ) = runExport("فاتورة_${transaction.id}.pdf") {
        ExportManager.generateInvoicePdf(cacheDir, transaction, items, storeName)
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

    fun exportInventoryExcel(
        products: List<ProductWithUnits>,
        storeName: String,
        cacheDir: File
    ) = runExport("تقرير_المخزون.xlsx") {
        ExportManager.generateInventoryExcel(cacheDir, products, storeName)
    }

    fun dismissError() { _state.value = ExportState.Idle }
    fun reset()        { _state.value = ExportState.Idle }

    // ── منطق داخلي ───────────────────────────────────────────

    private fun runExport(fileName: String, block: suspend () -> File) {
        if (_state.value is ExportState.Loading) return  // منع التكرار
        viewModelScope.launch {
            _state.value = ExportState.Loading("جارٍ تحضير $fileName...")
            runCatching { withContext(Dispatchers.IO) { block() } }
                .fold(
                    onSuccess = { file ->
                        val type = if (fileName.endsWith(".pdf")) ExportType.PDF else ExportType.XLSX
                        _state.value = ExportState.Success(
                            fileName = fileName,
                            filePath = file.absolutePath,
                            type     = type
                        )
                    },
                    onFailure = { e ->
                        _state.value = ExportState.Error(
                            message = when (e) {
                                is OutOfMemoryError  -> "لا توجد ذاكرة كافية"
                                is java.io.IOException -> "خطأ في الكتابة: ${e.message}"
                                else                 -> "فشل التصدير: ${e.message}"
                            },
                            cause = e
                        )
                    }
                )
        }
    }
}
