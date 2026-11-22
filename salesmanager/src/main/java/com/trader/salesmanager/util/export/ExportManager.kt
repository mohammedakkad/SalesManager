package com.trader.salesmanager.util.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.StockLevel
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.model.stockLevel
import com.trader.salesmanager.ui.reports.CustomerRank
import com.trader.salesmanager.ui.reports.DaySalesEntry
import com.trader.salesmanager.ui.reports.PaymentShare
import com.trader.salesmanager.util.pdf.CustomerStatementPdfGenerator
import com.trader.salesmanager.util.pdf.InvoicePdfGenerator
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير التصدير الشامل — PDF + Excel بدون مكتبات خارجية.
 *
 * PDF: يستخدم android.graphics.pdf.PdfDocument مع Canvas
 * Excel: يستخدم XlsxWriter المخصص (ZIP + OpenXML)
 */
object ExportManager {

    // ── ألوان المشروع ─────────────────────────────────────────


    // ══════════════════════════════════════════════════════════
    // 1. PDF — فاتورة عملية واحدة
    // ══════════════════════════════════════════════════════════
    fun generateInvoicePdf(
        cacheDir: File,
        transaction: Transaction,
        items: List<InvoiceItem>,
        storeName: String,
        customer: Customer? = null,
        logo: Bitmap? = null,
        priorDebtBalance: Double? = null,
        currentDebtBalance: Double? = null,
        returnSummary: ReturnSummary = ReturnSummary.NONE
    ): File = InvoicePdfGenerator.generate(
        cacheDir = cacheDir,
        transaction = transaction,
        items = items,
        customer = customer,
        storeName = storeName,
        logo = logo,
        priorDebtBalance = priorDebtBalance,
        currentDebtBalance = currentDebtBalance,
        returnSummary = returnSummary
    )

    fun generateMonthlyReportPdf(
        cacheDir: File,
        data: ReportPdfGenerator.MonthlyReportPdfData
    ): File = ReportPdfGenerator.generate(cacheDir, data)

    // ══════════════════════════════════════════════════════════
    // 2. PDF — كشف حساب عميل
    // ══════════════════════════════════════════════════════════
    fun generateCustomerStatementPdf(
        cacheDir: File,
        customer: Customer,
        transactions: List<Transaction>,
        storeName: String
    ): File = CustomerStatementPdfGenerator.generate(cacheDir, customer, transactions, storeName)

    // ══════════════════════════════════════════════════════════
    // 3. Excel — تقرير المبيعات
    // ══════════════════════════════════════════════════════════
    fun generateSalesReportExcel(
        cacheDir: File,
        transactions: List<Transaction>,
        periodLabel: String,
        storeName: String,
        dailySales: List<DaySalesEntry>,
        topSpenders: List<CustomerRank>,
        paymentShares: List<PaymentShare>
    ): File {
        val xlsx = XlsxWriter()

        val headerStyle = XlsxWriter.CellStyle(bold = true, bgColor = "FF10B981", fontColor = "FFFFFFFF", fontSize = 12)
        val subHeaderStyle = XlsxWriter.CellStyle(bold = true, bgColor = "FF064E3B", fontColor = "FFFFFFFF", fontSize = 11)
        val paidStyle   = XlsxWriter.CellStyle(bgColor = "FFD1FAE5", fontColor = "FF065F46")
        val debtStyle   = XlsxWriter.CellStyle(bgColor = "FFFEE2E2", fontColor = "FF7F1D1D")
        val totalStyle  = XlsxWriter.CellStyle(bold = true, bgColor = "FFF1F5F9", fontSize = 11)
        val altStyle    = XlsxWriter.CellStyle(bgColor = "FFF8FAFC")
        val normalStyle = XlsxWriter.CellStyle()
        val titleStyle  = XlsxWriter.CellStyle(bold = true, fontSize = 14)
        val emptyStyle  = XlsxWriter.CellStyle()

        xlsx.setColWidth(0, 8.0)
        xlsx.setColWidth(1, 22.0)
        xlsx.setColWidth(2, 18.0)
        xlsx.setColWidth(3, 15.0)
        xlsx.setColWidth(4, 15.0)
        xlsx.setColWidth(5, 15.0)
        xlsx.setColWidth(6, 15.0)

        // ── العنوان ────────────────────────────────────────────
        xlsx.addRow("#" to emptyStyle, "تقرير المبيعات — $storeName" to titleStyle)
        xlsx.addRow("#" to emptyStyle, "الفترة: $periodLabel" to XlsxWriter.CellStyle(fontColor = "FF64748B"))
        xlsx.addEmptyRow()

        // ── ملخص ───────────────────────────────────────────────
        xlsx.addRow("#" to emptyStyle, "ملخص الفترة" to subHeaderStyle)
        val total  = transactions.sumOf { it.amount }
        val paid   = transactions.filter { it.isPaid }.sumOf { it.amount }
        val unpaid = total - paid
        xlsx.addRow("#" to emptyStyle, "الإجمالي" to totalStyle, null to emptyStyle, total to totalStyle)
        xlsx.addRow("#" to emptyStyle, "مدفوع" to paidStyle, null to emptyStyle, paid to paidStyle)
        xlsx.addRow("#" to emptyStyle, "غير مدفوع" to debtStyle, null to emptyStyle, unpaid to debtStyle)
        xlsx.addRow("#" to emptyStyle, "عدد العمليات" to totalStyle, null to emptyStyle, transactions.size to totalStyle)
        xlsx.addEmptyRow()

        // ── جدول العمليات ──────────────────────────────────────
        xlsx.addRow("#" to headerStyle, "الزبون" to headerStyle, "التاريخ" to headerStyle,
            "المبلغ (₪)" to headerStyle, "طريقة الدفع" to headerStyle, "الحالة" to headerStyle, "ملاحظة" to headerStyle)

        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
        transactions.forEachIndexed { i, tx ->
            val rowStyle = if (tx.isPaid) paidStyle else debtStyle
            val alt = if (i % 2 == 0) normalStyle else altStyle
            xlsx.addRow(
                (i + 1).toString() to (if (tx.isPaid) paidStyle else debtStyle),
                tx.customerName to rowStyle,
                df.format(Date(tx.date)) to rowStyle,
                tx.amount to rowStyle,
                tx.paymentMethodName.ifEmpty { "—" } to rowStyle,
                (if (tx.isPaid) "مدفوع" else "غير مدفوع") to rowStyle,
                tx.note.ifEmpty { "—" } to rowStyle
            )
        }
        xlsx.addEmptyRow()

        // ── المبيعات اليومية ───────────────────────────────────
        if (dailySales.isNotEmpty()) {
            xlsx.addRow("#" to emptyStyle, "المبيعات اليومية" to subHeaderStyle)
            xlsx.addRow("#" to emptyStyle, "اليوم" to headerStyle, "الإجمالي (₪)" to headerStyle, "مدفوع (₪)" to headerStyle)
            dailySales.forEach { d ->
                xlsx.addRow("#" to emptyStyle, d.label to normalStyle, d.total to normalStyle, d.paid to paidStyle)
            }
            xlsx.addEmptyRow()
        }

        // ── أفضل العملاء ───────────────────────────────────────
        if (topSpenders.isNotEmpty()) {
            xlsx.addRow("#" to emptyStyle, "أكثر العملاء شراءً" to subHeaderStyle)
            xlsx.addRow("#" to emptyStyle, "العميل" to headerStyle, "المجموع (₪)" to headerStyle)
            topSpenders.forEachIndexed { i, r ->
                xlsx.addRow((i+1).toString() to normalStyle, r.name to normalStyle, r.amount to normalStyle)
            }
            xlsx.addEmptyRow()
        }

        // ── توزيع طرق الدفع ────────────────────────────────────
        if (paymentShares.isNotEmpty()) {
            xlsx.addRow("#" to emptyStyle, "توزيع طرق الدفع" to subHeaderStyle)
            xlsx.addRow("#" to emptyStyle, "الطريقة" to headerStyle, "المجموع (₪)" to headerStyle)
            paymentShares.forEachIndexed { i, p ->
                xlsx.addRow((i+1).toString() to normalStyle, p.name to normalStyle, p.amount to normalStyle)
            }
        }

        val file = File(cacheDir, "sales_report_${System.currentTimeMillis()}.xlsx")
        xlsx.write(file)
        return file
    }

    // ══════════════════════════════════════════════════════════
    // 4. Excel — تقرير المخزون
    // ══════════════════════════════════════════════════════════
    fun generateInventoryExcel(
        cacheDir: File,
        products: List<ProductWithUnits>,
        storeName: String
    ): File {
        val xlsx = XlsxWriter()

        val headerStyle = XlsxWriter.CellStyle(bold = true, bgColor = "FF0EA5E9", fontColor = "FFFFFFFF", fontSize = 11)
        val greenStyle  = XlsxWriter.CellStyle(bgColor = "FFD1FAE5", fontColor = "FF065F46")
        val yellowStyle = XlsxWriter.CellStyle(bgColor = "FFFEF9C3", fontColor = "FF78350F")
        val redStyle    = XlsxWriter.CellStyle(bgColor = "FFFEE2E2", fontColor = "FF7F1D1D")
        val normalStyle = XlsxWriter.CellStyle()
        val altStyle    = XlsxWriter.CellStyle(bgColor = "FFF8FAFC")
        val titleStyle  = XlsxWriter.CellStyle(bold = true, fontSize = 14)

        xlsx.setColWidth(0, 6.0)
        xlsx.setColWidth(1, 25.0)
        xlsx.setColWidth(2, 15.0)
        xlsx.setColWidth(3, 12.0)
        xlsx.setColWidth(4, 12.0)
        xlsx.setColWidth(5, 12.0)
        xlsx.setColWidth(6, 15.0)
        xlsx.setColWidth(7, 12.0)

        xlsx.addRow("" to normalStyle, "تقرير المخزون — $storeName" to titleStyle)
        xlsx.addRow("" to normalStyle, SimpleDateFormat("dd MMMM yyyy", Locale("ar")).format(Date()) to XlsxWriter.CellStyle(fontColor = "FF64748B"))
        xlsx.addEmptyRow()

        xlsx.addRow(
            "#" to headerStyle, "اسم الصنف" to headerStyle, "الفئة" to headerStyle,
            "الوحدة" to headerStyle, "الكمية" to headerStyle, "السعر (₪)" to headerStyle,
            "القيمة الإجمالية" to headerStyle, "الحالة" to headerStyle
        )

        var rowIdx = 0
        products.forEach { pw ->
            pw.units.forEach { unit ->
                val isLow  = unit.stockLevel == StockLevel.LOW
                val isOut  = unit.stockLevel == StockLevel.OUT
                val rowStyle = when {
                    isOut  -> redStyle
                    isLow  -> yellowStyle
                    else   -> if (rowIdx % 2 == 0) normalStyle else altStyle
                }
                val status = when {
                    isOut  -> "نفد"
                    isLow  -> "منخفض"
                    else   -> "متوفر"
                }
                xlsx.addRow(
                    (rowIdx + 1).toString() to rowStyle,
                    pw.product.name to rowStyle,
                    pw.product.category.ifEmpty { "—" } to rowStyle,
                    unit.unitLabel to rowStyle,
                    unit.quantityInStock to rowStyle,
                    unit.price to rowStyle,
                    (unit.quantityInStock * unit.price) to rowStyle,
                    status to rowStyle
                )
                rowIdx++
            }
        }

        // ملخص
        xlsx.addEmptyRow()
        val totalValue = products.sumOf { pw -> pw.units.sumOf { it.quantityInStock * it.price } }
        val outCount   = products.count { pw -> pw.units.all { it.stockLevel == StockLevel.OUT } }
        val lowCount   = products.count { pw -> pw.units.any { it.stockLevel == StockLevel.LOW } }
        val sumStyle   = XlsxWriter.CellStyle(bold = true, bgColor = "FF0EA5E9", fontColor = "FFFFFFFF")
        xlsx.addRow("" to normalStyle, "إجمالي قيمة المخزون" to sumStyle, null to normalStyle, null to normalStyle, null to normalStyle, null to normalStyle, totalValue to sumStyle)
        xlsx.addRow("" to normalStyle, "أصناف نفدت" to redStyle, outCount to redStyle)
        xlsx.addRow("" to normalStyle, "أصناف منخفضة" to yellowStyle, lowCount to yellowStyle)

        val file = File(cacheDir, "inventory_${System.currentTimeMillis()}.xlsx")
        xlsx.write(file)
        return file
    }

    // ══════════════════════════════════════════════════════════
    // مشاركة الملفات
    // ══════════════════════════════════════════════════════════
    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الملف").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareToWhatsApp(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            shareFile(context, file, mimeType)
        }
    }

    fun saveToDownloads(context: Context, file: File, displayName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val mimeType = if (file.extension == "pdf") "application/pdf" else
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                var published = false
                try {
                    val output = resolver.openOutputStream(uri) ?: return false
                    output.use { out -> file.inputStream().use { input -> input.copyTo(out) } }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    published = resolver.update(uri, values, null, null) > 0
                    published
                } finally {
                    if (!published) runCatching { resolver.delete(uri, null, null) }
                }
            } else {
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), displayName)
                file.copyTo(dest, overwrite = true)
                true
            }
        } catch (_: Exception) { false }
    }
}
