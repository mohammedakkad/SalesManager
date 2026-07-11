package com.trader.salesmanager.util.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.inventory.invoice.formatAmount
import com.trader.salesmanager.ui.inventory.invoice.formatQty
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object InvoicePdfGenerator {
    data class InvoicePdfData(
        val transaction: Transaction,
        val items: List<InvoiceItem>,
        val customer: Customer?,
        val storeName: String,
        val logo: Bitmap? = null,
        val priorDebtBalance: Double? = null,
        val currentDebtBalance: Double? = null,
        val returnSummary: ReturnSummary = ReturnSummary.NONE
    )

    fun generate(
        cacheDir: File,
        transaction: Transaction,
        items: List<InvoiceItem>,
        customer: Customer?,
        storeName: String,
        logo: Bitmap? = null,
        priorDebtBalance: Double? = null,
        currentDebtBalance: Double? = null,
        returnSummary: ReturnSummary = ReturnSummary.NONE
    ): File = generate(
        cacheDir,
        InvoicePdfData(
            transaction = transaction,
            items = items,
            customer = customer,
            storeName = storeName,
            logo = logo,
            priorDebtBalance = priorDebtBalance,
            currentDebtBalance = currentDebtBalance,
            returnSummary = returnSummary
        )
    )

    fun generate(cacheDir: File, data: InvoicePdfData): File {
        val document = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNumber = 0
        var y = 0f
        val generatedAt = System.currentTimeMillis()
        val displayStoreName = data.storeName.ifBlank { "المتجر" }
        val documentTitle = if (data.transaction.paymentType == PaymentType.DEBT) {
            "مبيع آجل"
        } else {
            "فاتورة مبيعات"
        }
        val invoiceNumber = "INV-${data.transaction.id.toString().padStart(6, '0')}"

        fun finishPage() {
            page?.let(document::finishPage)
            page = null
            canvas = null
        }

        fun startPage(continuation: Boolean = pageNumber > 0) {
            finishPage()
            pageNumber += 1
            val pageInfo = PdfDocument.PageInfo.Builder(
                PdfLayoutConstants.PAGE_WIDTH,
                PdfLayoutConstants.PAGE_HEIGHT,
                pageNumber
            ).create()
            page = document.startPage(pageInfo)
            canvas = page!!.canvas
            drawHeader(
                canvas = canvas!!,
                storeName = displayStoreName,
                documentTitle = documentTitle,
                invoiceNumber = invoiceNumber,
                logo = data.logo,
                continuation = continuation
            )
            drawFooter(canvas!!, displayStoreName, generatedAt, pageNumber)
            y = if (continuation) {
                PdfLayoutConstants.CONTINUATION_HEADER_HEIGHT + 14f
            } else {
                PdfLayoutConstants.HEADER_HEIGHT + 14f
            }
        }

        fun ensureSpace(height: Float) {
            if (y + height > PdfLayoutConstants.CONTENT_BOTTOM) startPage(true)
        }

        fun drawTableHeader() {
            val target = canvas!!
            target.drawRect(
                PdfLayoutConstants.MARGIN,
                y,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                y + PdfLayoutConstants.TABLE_HEADER_HEIGHT,
                PdfCanvas.fill(PdfLayoutConstants.EMERALD_700)
            )
            drawTableCells(
                canvas = target,
                top = y,
                height = PdfLayoutConstants.TABLE_HEADER_HEIGHT,
                values = listOf("الإجمالي", "السعر", "الكمية", "الوحدة", "اسم المنتج"),
                color = PdfLayoutConstants.WHITE,
                bold = true
            )
            y += PdfLayoutConstants.TABLE_HEADER_HEIGHT
        }

        try {
            startPage(false)
            y = drawInvoiceDetails(
                canvas = canvas!!,
                top = y,
                data = data,
                invoiceNumber = invoiceNumber
            )
            y += 14f
            PdfCanvas.text(
                canvas!!,
                "تفاصيل الأصناف",
                PdfLayoutConstants.MARGIN,
                y,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                13f,
                PdfLayoutConstants.SLATE_900,
                bold = true
            )
            y += 22f
            drawTableHeader()

            if (data.items.isEmpty()) {
                val emptyHeight = 48f
                PdfCanvas.roundRect(
                    canvas!!,
                    PdfLayoutConstants.MARGIN,
                    y,
                    PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                    y + emptyHeight,
                    PdfLayoutConstants.SLATE_50,
                    borderColor = PdfLayoutConstants.SLATE_200
                )
                PdfCanvas.cellText(
                    canvas!!,
                    "فاتورة بمبلغ يدوي دون أصناف مسجلة",
                    PdfLayoutConstants.MARGIN,
                    y,
                    PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                    emptyHeight,
                    10f,
                    PdfLayoutConstants.SLATE_600
                )
                y += emptyHeight
            } else {
                data.items.forEachIndexed { index, item ->
                    val returnedQuantity = data.returnSummary.returnedByUnit[item.unitId] ?: 0.0
                    val netQuantity = (item.quantity - returnedQuantity).coerceAtLeast(0.0)
                    val productWidth = tableWidths.last() - 8f
                    val rowHeight = max(
                        PdfLayoutConstants.TABLE_ROW_MIN_HEIGHT,
                        PdfCanvas.measureHeight(
                            if (returnedQuantity > 0.0) "${item.productName} (بعد الإرجاع)" else item.productName,
                            productWidth,
                            9f,
                            maxLines = 2
                        ) + 12f
                    )
                    if (y + rowHeight > PdfLayoutConstants.CONTENT_BOTTOM) {
                        startPage(true)
                        PdfCanvas.text(
                            canvas!!,
                            "تفاصيل الأصناف - تابع",
                            PdfLayoutConstants.MARGIN,
                            y,
                            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                            12f,
                            PdfLayoutConstants.SLATE_900,
                            bold = true
                        )
                        y += 21f
                        drawTableHeader()
                    }
                    val background = if (index % 2 == 0) {
                        PdfLayoutConstants.WHITE
                    } else {
                        PdfLayoutConstants.SLATE_50
                    }
                    canvas!!.drawRect(
                        PdfLayoutConstants.MARGIN,
                        y,
                        PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                        y + rowHeight,
                        PdfCanvas.fill(background)
                    )
                    canvas!!.drawRect(
                        PdfLayoutConstants.MARGIN,
                        y,
                        PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                        y + rowHeight,
                        PdfCanvas.stroke(PdfLayoutConstants.SLATE_200, 0.6f)
                    )
                    drawTableCells(
                        canvas = canvas!!,
                        top = y,
                        height = rowHeight,
                        values = listOf(
                            "₪ ${(netQuantity * item.pricePerUnit).formatAmount()}",
                            "₪ ${item.pricePerUnit.formatAmount()}",
                            netQuantity.formatQty(),
                            item.unitLabel.ifBlank { "—" },
                            (
                                if (returnedQuantity > 0.0) {
                                    "${item.productName} (بعد الإرجاع)"
                                } else {
                                    item.productName
                                }
                            ).ifBlank { "—" }
                        ),
                        color = PdfLayoutConstants.SLATE_700,
                        bold = false
                    )
                    y += rowHeight
                }
            }

            ensureSpace(120f)
            y += 14f
            y = drawTotals(canvas!!, y, data)

            ensureSpace(if (data.transaction.paymentType == PaymentType.DEBT) 190f else 112f)
            y += 14f
            y = if (data.transaction.paymentType == PaymentType.DEBT) {
                drawDebtSection(canvas!!, y, data)
            } else {
                drawPaymentSection(canvas!!, y, data.transaction)
            }

            if (data.transaction.note.isNotBlank()) {
                val noteHeight = max(
                    64f,
                    PdfCanvas.measureHeight(
                        data.transaction.note,
                        PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 24f,
                        10f,
                        maxLines = 4
                    ) + 40f
                )
                ensureSpace(noteHeight + 14f)
                y += 14f
                y = drawNotes(canvas!!, y, data.transaction.note, noteHeight)
            }

            ensureSpace(70f)
            y += 16f
            drawSignature(canvas!!, y)
            finishPage()

            val output = File(cacheDir, "invoice_${data.transaction.id}.pdf")
            FileOutputStream(output).use(document::writeTo)
            return output
        } finally {
            if (page != null) finishPage()
            document.close()
        }
    }

    private val tableWidths = listOf(92f, 82f, 62f, 72f, 215f)

    private fun drawHeader(
        canvas: Canvas,
        storeName: String,
        documentTitle: String,
        invoiceNumber: String,
        logo: Bitmap?,
        continuation: Boolean
    ) {
        val height = if (continuation) {
            PdfLayoutConstants.CONTINUATION_HEADER_HEIGHT
        } else {
            PdfLayoutConstants.HEADER_HEIGHT
        }
        canvas.drawRect(
            0f,
            0f,
            PdfLayoutConstants.PAGE_WIDTH.toFloat(),
            height,
            PdfCanvas.fill(PdfLayoutConstants.EMERALD_700)
        )
        PdfCanvas.text(
            canvas,
            storeName,
            130f,
            if (continuation) 16f else 20f,
            PdfLayoutConstants.PAGE_WIDTH - 130f - PdfLayoutConstants.MARGIN,
            if (continuation) 17f else 21f,
            PdfLayoutConstants.WHITE,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            if (continuation) "$documentTitle - تابع" else documentTitle,
            130f,
            if (continuation) 40f else 51f,
            PdfLayoutConstants.PAGE_WIDTH - 130f - PdfLayoutConstants.MARGIN,
            11f,
            PdfLayoutConstants.EMERALD_100
        )
        PdfCanvas.text(
            canvas,
            invoiceNumber,
            PdfLayoutConstants.MARGIN,
            if (continuation) 40f else 72f,
            150f,
            10f,
            PdfLayoutConstants.WHITE,
            bold = true,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
        if (!continuation) drawLogo(canvas, logo)
    }

    private fun drawLogo(canvas: Canvas, logo: Bitmap?) {
        val centerX = 76f
        val centerY = 43f
        val radius = 29f
        canvas.drawCircle(centerX, centerY, radius, PdfCanvas.fill(PdfLayoutConstants.CYAN_500))
        if (logo != null) {
            val path = Path().apply { addCircle(centerX, centerY, radius - 2f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(
                logo,
                Rect(0, 0, logo.width, logo.height),
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                PdfCanvas.fill(PdfLayoutConstants.WHITE)
            )
            canvas.restore()
        } else {
            PdfCanvas.cellText(
                canvas,
                "SM",
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                16f,
                PdfLayoutConstants.WHITE,
                bold = true
            )
        }
    }

    private fun drawInvoiceDetails(
        canvas: Canvas,
        top: Float,
        data: InvoicePdfData,
        invoiceNumber: String
    ): Float {
        val left = PdfLayoutConstants.MARGIN
        val right = PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN
        val height = 118f
        PdfCanvas.roundRect(
            canvas,
            left,
            top,
            right,
            top + height,
            PdfLayoutConstants.SLATE_50,
            borderColor = PdfLayoutConstants.SLATE_200
        )
        val date = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))
            .format(Date(data.transaction.date))
        val rows = listOf(
            "رقم الفاتورة" to invoiceNumber,
            "التاريخ" to date,
            "اسم العميل" to (data.customer?.name ?: data.transaction.customerName).ifBlank { "زبون زائر" },
            "رقم الهاتف" to data.customer?.phone.orEmpty().ifBlank { "—" }
        )
        rows.forEachIndexed { index, row ->
            val rowTop = top + 12f + index * 25f
            PdfCanvas.text(
                canvas,
                "${row.first}:",
                right - 118f,
                rowTop,
                104f,
                9f,
                PdfLayoutConstants.SLATE_600,
                bold = true
            )
            PdfCanvas.text(
                canvas,
                row.second,
                left + 14f,
                rowTop,
                right - left - 138f,
                10f,
                PdfLayoutConstants.SLATE_900,
                maxLines = 1
            )
            if (index < rows.lastIndex) {
                canvas.drawLine(
                    left + 12f,
                    rowTop + 17f,
                    right - 12f,
                    rowTop + 17f,
                    PdfCanvas.stroke(PdfLayoutConstants.SLATE_200, 0.5f)
                )
            }
        }
        return top + height
    }

    private fun drawTableCells(
        canvas: Canvas,
        top: Float,
        height: Float,
        values: List<String>,
        color: Int,
        bold: Boolean
    ) {
        var left = PdfLayoutConstants.MARGIN
        values.forEachIndexed { index, value ->
            val width = tableWidths[index]
            PdfCanvas.cellText(
                canvas,
                value,
                left,
                top,
                width,
                height,
                if (index == values.lastIndex) 9.2f else 8.8f,
                if (!bold && index == 0) PdfLayoutConstants.EMERALD_700 else color,
                bold = bold || (!bold && index == 0),
                alignment = if (index == values.lastIndex) {
                    Layout.Alignment.ALIGN_NORMAL
                } else {
                    Layout.Alignment.ALIGN_CENTER
                }
            )
            if (index < values.lastIndex) {
                canvas.drawLine(
                    left + width,
                    top,
                    left + width,
                    top + height,
                    PdfCanvas.stroke(
                        if (bold) PdfLayoutConstants.EMERALD_500 else PdfLayoutConstants.SLATE_200,
                        0.5f
                    )
                )
            }
            left += width
        }
    }

    private fun drawTotals(canvas: Canvas, top: Float, data: InvoicePdfData): Float {
        val left = PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 265f
        val right = PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN
        val returned = data.returnSummary.totalRefunded.coerceAtLeast(0.0)
        val subtotal = data.transaction.amount + returned
        val height = if (returned > 0.0) 101f else 78f
        PdfCanvas.roundRect(
            canvas,
            left,
            top,
            right,
            top + height,
            PdfLayoutConstants.EMERALD_100,
            borderColor = PdfLayoutConstants.EMERALD_500
        )
        drawKeyValue(canvas, left + 12f, right - 12f, top + 13f, "المجموع", "₪ ${subtotal.formatAmount()}")
        if (returned > 0.0) {
            drawKeyValue(
                canvas,
                left + 12f,
                right - 12f,
                top + 38f,
                "المرتجع",
                "- ₪ ${returned.formatAmount()}"
            )
        }
        val dividerTop = if (returned > 0.0) top + 61f else top + 38f
        canvas.drawLine(
            left + 12f,
            dividerTop,
            right - 12f,
            dividerTop,
            PdfCanvas.stroke(PdfLayoutConstants.EMERALD_500, 0.6f)
        )
        drawKeyValue(
            canvas,
            left + 12f,
            right - 12f,
            dividerTop + 11f,
            "الإجمالي",
            "₪ ${data.transaction.amount.formatAmount()}",
            emphasized = true
        )
        return top + height
    }

    private fun drawPaymentSection(canvas: Canvas, top: Float, transaction: Transaction): Float {
        val height = 94f
        drawSectionCard(canvas, top, height, "تفاصيل الدفع", PdfLayoutConstants.CYAN_100)
        val paymentLabel = paymentTypeLabel(transaction.paymentType, transaction.paymentMethodName)
        val paid = if (transaction.isPaid) transaction.amount else 0.0
        val remaining = (transaction.amount - paid).coerceAtLeast(0.0)
        drawKeyValue(canvas, PdfLayoutConstants.MARGIN + 14f, PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 14f, top + 38f, "طريقة الدفع", paymentLabel)
        drawKeyValue(canvas, PdfLayoutConstants.MARGIN + 14f, PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 14f, top + 61f, "المدفوع", "₪ ${paid.formatAmount()}")
        drawKeyValue(canvas, PdfLayoutConstants.MARGIN + 14f, PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 14f, top + 78f, "المتبقي", "₪ ${remaining.formatAmount()}")
        return top + height
    }

    private fun drawDebtSection(canvas: Canvas, top: Float, data: InvoicePdfData): Float {
        val transaction = data.transaction
        val rows = mutableListOf<Pair<String, String>>()
        data.priorDebtBalance?.let { rows += "رصيده قبل الفاتورة" to "₪ ${it.formatAmount()}" }
        rows += "صافي الفاتورة" to "₪ ${transaction.amount.formatAmount()}"
        rows += "المتبقي" to "₪ ${(if (transaction.isPaid) 0.0 else transaction.amount).formatAmount()}"
        data.currentDebtBalance?.let { rows += "رصيد الحساب الحالي" to "₪ ${it.formatAmount()}" }
        transaction.dueDate?.let {
            rows += "تاريخ الاستحقاق" to SimpleDateFormat("dd/MM/yyyy", Locale("ar")).format(Date(it))
        }
        rows += "حالة الدين" to if (transaction.isPaid) "مسدد" else "غير مسدد"
        val height = 38f + rows.size * 22f
        drawSectionCard(canvas, top, height, "تفاصيل البيع الآجل", PdfLayoutConstants.AMBER_100)
        rows.forEachIndexed { index, row ->
            drawKeyValue(
                canvas,
                PdfLayoutConstants.MARGIN + 14f,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 14f,
                top + 38f + index * 22f,
                row.first,
                row.second,
                emphasized = row.first == "المتبقي" || row.first == "رصيد الحساب الحالي"
            )
        }
        return top + height
    }

    private fun drawSectionCard(
        canvas: Canvas,
        top: Float,
        height: Float,
        title: String,
        background: Int
    ) {
        PdfCanvas.roundRect(
            canvas,
            PdfLayoutConstants.MARGIN,
            top,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
            top + height,
            background,
            borderColor = PdfLayoutConstants.SLATE_200
        )
        PdfCanvas.text(
            canvas,
            title,
            PdfLayoutConstants.MARGIN + 14f,
            top + 12f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 28f,
            12f,
            PdfLayoutConstants.SLATE_900,
            bold = true
        )
    }

    private fun drawNotes(canvas: Canvas, top: Float, note: String, height: Float): Float {
        drawSectionCard(canvas, top, height, "ملاحظات", PdfLayoutConstants.SLATE_50)
        PdfCanvas.text(
            canvas,
            note,
            PdfLayoutConstants.MARGIN + 14f,
            top + 36f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 28f,
            10f,
            PdfLayoutConstants.SLATE_700,
            maxLines = 4
        )
        return top + height
    }

    private fun drawSignature(canvas: Canvas, top: Float) {
        val left = PdfLayoutConstants.MARGIN
        val right = PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN
        PdfCanvas.text(
            canvas,
            "التوقيع",
            right - 180f,
            top,
            180f,
            10f,
            PdfLayoutConstants.SLATE_600,
            bold = true
        )
        canvas.drawLine(
            right - 180f,
            top + 32f,
            right,
            top + 32f,
            PdfCanvas.stroke(PdfLayoutConstants.SLATE_400)
        )
        PdfCanvas.text(
            canvas,
            "شكراً لتعاملكم معنا",
            left,
            top + 11f,
            220f,
            10f,
            PdfLayoutConstants.EMERALD_700,
            bold = true,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
    }

    private fun drawKeyValue(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        label: String,
        value: String,
        emphasized: Boolean = false
    ) {
        PdfCanvas.text(
            canvas,
            label,
            right - 130f,
            top,
            130f,
            if (emphasized) 11f else 9.5f,
            PdfLayoutConstants.SLATE_700,
            bold = emphasized
        )
        PdfCanvas.text(
            canvas,
            value,
            left,
            top,
            right - left - 142f,
            if (emphasized) 12f else 9.5f,
            if (emphasized) PdfLayoutConstants.EMERALD_700 else PdfLayoutConstants.SLATE_900,
            bold = emphasized,
            alignment = Layout.Alignment.ALIGN_OPPOSITE,
            maxLines = 1
        )
    }

    private fun drawFooter(
        canvas: Canvas,
        storeName: String,
        generatedAt: Long,
        pageNumber: Int
    ) {
        val top = PdfLayoutConstants.PAGE_HEIGHT - PdfLayoutConstants.FOOTER_HEIGHT
        canvas.drawRect(
            0f,
            top,
            PdfLayoutConstants.PAGE_WIDTH.toFloat(),
            PdfLayoutConstants.PAGE_HEIGHT.toFloat(),
            PdfCanvas.fill(PdfLayoutConstants.SLATE_100)
        )
        canvas.drawLine(
            0f,
            top,
            PdfLayoutConstants.PAGE_WIDTH.toFloat(),
            top,
            PdfCanvas.stroke(PdfLayoutConstants.SLATE_200)
        )
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
            .format(Date(generatedAt))
        PdfCanvas.text(
            canvas,
            storeName,
            PdfLayoutConstants.PAGE_WIDTH / 2f,
            top + 12f,
            PdfLayoutConstants.PAGE_WIDTH / 2f - PdfLayoutConstants.MARGIN,
            8f,
            PdfLayoutConstants.SLATE_600,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            "طبع بتاريخ $timestamp · صفحة $pageNumber",
            PdfLayoutConstants.MARGIN,
            top + 12f,
            PdfLayoutConstants.PAGE_WIDTH / 2f - PdfLayoutConstants.MARGIN,
            8f,
            PdfLayoutConstants.SLATE_600,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
    }

    private fun paymentTypeLabel(type: PaymentType, methodName: String): String {
        val typeLabel = when (type) {
            PaymentType.CASH -> "نقداً"
            PaymentType.DEBT -> "دين"
            PaymentType.BANK -> "تحويل بنكي"
            PaymentType.WALLET -> "محفظة إلكترونية"
            PaymentType.OTHER -> "أخرى"
        }
        return methodName.takeIf { it.isNotBlank() }?.let { "$typeLabel - $it" } ?: typeLabel
    }
}
