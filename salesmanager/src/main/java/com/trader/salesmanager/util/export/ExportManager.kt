package com.trader.salesmanager.util.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.reports.CustomerRank
import com.trader.salesmanager.ui.reports.DaySalesEntry
import com.trader.salesmanager.ui.reports.PaymentShare
import com.trader.salesmanager.util.pdf.InvoicePdfGenerator
import com.trader.salesmanager.util.pdf.PdfLayoutConstants
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import java.io.File
import java.io.FileOutputStream
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
    private val GREEN   = PdfLayoutConstants.EMERALD_500
    private val GREEN_L = PdfLayoutConstants.EMERALD_100
    private val RED     = PdfLayoutConstants.RED_500
    private val RED_L   = PdfLayoutConstants.RED_100
    private val AMBER   = PdfLayoutConstants.AMBER_500
    private val AMBER_L = PdfLayoutConstants.AMBER_100
    private val VIOLET  = PdfLayoutConstants.VIOLET_500
    private val CYAN    = PdfLayoutConstants.CYAN_500
    private val GRAY_L  = PdfLayoutConstants.SLATE_50
    private val GRAY_M  = PdfLayoutConstants.SLATE_200
    private val GRAY_D  = PdfLayoutConstants.SLATE_600
    private val BLACK   = PdfLayoutConstants.SLATE_900
    private val WHITE   = PdfLayoutConstants.WHITE

    // A4 dimensions at 72 DPI
    private const val W = 595f
    private const val H = 842f
    private const val M = 36f  // margin

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
    ): File {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), 1).create()
        val page = doc.startPage(info)
        val c = page.canvas
        var y = 0f

        // Header
        c.drawRect(0f, 0f, W, 80f, fill(VIOLET))
        c.drawText(customer.name, W - M, 38f, paint(WHITE, 20f, bold = true, align = Paint.Align.RIGHT))
        c.drawText("كشف الحساب", W - M, 56f, paint(WHITE.withAlpha(200), 11f, align = Paint.Align.RIGHT))
        if (customer.phone.isNotEmpty())
            c.drawText("📞 ${customer.phone}", M, 56f, paint(WHITE.withAlpha(200), 10f, align = Paint.Align.LEFT))
        y = 90f

        // ملخص
        val total  = transactions.sumOf { it.amount }
        val paid   = transactions.filter { it.isPaid }.sumOf { it.amount }
        val unpaid = total - paid

        c.drawRect(M, y, W - M, y + 50f, fill(GRAY_L))
        val third = (W - M * 2) / 3
        summaryBox(c, M,           y, third, "الإجمالي", "₪${total.fmt()}",  GRAY_D)
        summaryBox(c, M + third,   y, third, "مدفوع",    "₪${paid.fmt()}",   GREEN)
        summaryBox(c, M + third*2, y, third, "دين",      "₪${unpaid.fmt()}", RED)
        y += 58f

        // رأس جدول العمليات
        c.drawText("سجل العمليات", W - M, y + 14f, paint(BLACK, 13f, bold = true, align = Paint.Align.RIGHT))
        y += 22f

        c.drawRect(M, y, W - M, y + 26f, fill(VIOLET))
        val txFmt = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
        listOf("المبلغ" to 0.15f, "طريقة الدفع" to 0.3f, "الحالة" to 0.5f, "التاريخ" to 0.75f, "#" to 0.95f).forEach { (label, pct) ->
            c.drawText(label, M + (W - M*2) * pct, y + 18f, paint(WHITE, 10f, bold = true, align = Paint.Align.CENTER))
        }
        y += 26f

        transactions.take(25).forEachIndexed { i, tx ->
            if (y > H - 80f) return@forEachIndexed  // ضمان عدم التجاوز
            val bg = if (i % 2 == 0) WHITE else GRAY_L
            c.drawRect(M, y, W - M, y + 22f, fill(bg))
            c.drawRect(M, y, W - M, y + 22f, stroke(GRAY_M, 0.5f))

            val statusColor = if (tx.isPaid) GREEN else RED
            c.drawText("₪${tx.amount.fmt()}", M + (W-M*2)*0.15f, y+15f, paint(BLACK, 9f, align = Paint.Align.CENTER))
            c.drawText(tx.paymentMethodName.ifEmpty {"—"}, M+(W-M*2)*0.3f, y+15f, paint(GRAY_D,9f,align=Paint.Align.CENTER))
            c.drawText(if(tx.isPaid)"مدفوع" else "دين", M+(W-M*2)*0.5f, y+15f, paint(statusColor,9f,bold=true,align=Paint.Align.CENTER))
            c.drawText(txFmt.format(Date(tx.date)), M+(W-M*2)*0.75f, y+15f, paint(GRAY_D,9f,align=Paint.Align.CENTER))
            c.drawText("#${tx.id}", M+(W-M*2)*0.95f, y+15f, paint(GRAY_D,8f,align=Paint.Align.CENTER))
            y += 22f
        }

        if (transactions.size > 25) {
            c.drawText("... وأكثر (${transactions.size - 25} عملية إضافية)", W - M, y + 16f, paint(GRAY_D, 9f, align = Paint.Align.RIGHT))
        }

        drawFooter(c, storeName)

        doc.finishPage(page)
        val file = File(cacheDir, "statement_${customer.id}.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

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
                val isLow  = unit.quantityInStock in 0.001..unit.lowStockThreshold
                val isOut  = unit.quantityInStock <= 0.001
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
        val outCount   = products.count { pw -> pw.units.all { it.quantityInStock <= 0.001 } }
        val lowCount   = products.count { pw -> pw.units.any { it.quantityInStock in 0.001..it.lowStockThreshold } }
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
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { out -> file.inputStream().copyTo(out) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
                true
            } else {
                val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), displayName)
                file.copyTo(dest, overwrite = true)
                true
            }
        } catch (_: Exception) { false }
    }

    // ══════════════════════════════════════════════════════════
    // PDF helpers
    // ══════════════════════════════════════════════════════════
    private fun summaryBox(c: Canvas, x: Float, y: Float, w: Float, label: String, value: String, color: Int) {
        c.drawText(label, x + w - 5f, y + 16f, paint(GRAY_D, 9f, align = Paint.Align.RIGHT))
        c.drawText(value, x + w - 5f, y + 34f, paint(color, 12f, bold = true, align = Paint.Align.RIGHT))
    }

    private fun drawFooter(c: Canvas, storeName: String) {
        c.drawRect(0f, H - 35f, W, H, fill(PdfLayoutConstants.SLATE_100))
        c.drawLine(0f, H - 35f, W, H - 35f, stroke(GRAY_M))
        c.drawText("شكراً لتعاملكم معنا", W / 2, H - 15f, paint(GRAY_D, 9f, align = Paint.Align.CENTER))
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar")).format(Date())
        c.drawText("صدر بتاريخ: $now", M, H - 15f, paint(GRAY_D, 8f, align = Paint.Align.LEFT))
    }

    // ── Paint factory ─────────────────────────────────────────
    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.RIGHT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    private fun Double.fmt(): String = if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this)
}
