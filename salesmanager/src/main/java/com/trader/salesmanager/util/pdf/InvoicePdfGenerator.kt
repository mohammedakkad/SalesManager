package com.trader.salesmanager.util.pdf

import android.graphics.Bitmap
import android.text.Layout
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.model.TransactionReturnStatus
import com.trader.core.util.format.AppNumberFormat
import com.trader.salesmanager.util.pdf.components.PdfBadgeTone
import com.trader.salesmanager.util.pdf.components.PdfCell
import com.trader.salesmanager.util.pdf.components.PdfColumn
import com.trader.salesmanager.util.pdf.components.PdfInfoCard
import com.trader.salesmanager.util.pdf.components.PdfNoteCard
import com.trader.salesmanager.util.pdf.components.PdfSectionTitle
import com.trader.salesmanager.util.pdf.components.PdfStatusBadge
import com.trader.salesmanager.util.pdf.components.PdfSummaryCard
import com.trader.salesmanager.util.pdf.components.PdfSummaryRow
import com.trader.salesmanager.util.pdf.components.PdfTable
import com.trader.salesmanager.util.pdf.core.AndroidPdfPageSink
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.core.PdfHeaderContent
import com.trader.salesmanager.util.pdf.core.PdfPageSink
import com.trader.salesmanager.util.pdf.design.PdfOptions
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale
import java.io.File
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
        val returnSummary: ReturnSummary = ReturnSummary.NONE,
        val options: PdfOptions = PdfOptions.DEFAULT,
        val printedAt: Long = System.currentTimeMillis()
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
        returnSummary: ReturnSummary = ReturnSummary.NONE,
        options: PdfOptions = PdfOptions.DEFAULT
    ): File = generate(
        cacheDir,
        InvoicePdfData(transaction, items, customer, storeName, logo, priorDebtBalance, currentDebtBalance, returnSummary, options)
    )

    fun generate(cacheDir: File, data: InvoicePdfData): File {
        val sink = AndroidPdfPageSink()
        try {
            render(data, sink)
            return sink.writeTo(File(cacheDir, "invoice_${data.transaction.id}.pdf"))
        } finally {
            sink.close()
        }
    }

    /** Lays out and renders the invoice into [sink]; returns the builder (layout log) for verification. */
    fun render(data: InvoicePdfData, sink: PdfPageSink): PdfDocumentBuilder {
        val tx = data.transaction
        val isDebt = tx.paymentType == PaymentType.DEBT
        val date = AppNumberFormat.date(tx.date)
        val builder = PdfDocumentBuilder(
            PdfHeaderContent(
                storeName = data.storeName.ifBlank { "المتجر" },
                documentTitle = if (isDebt) "فاتورة بيع آجل" else "فاتورة مبيعات",
                documentNumber = AppNumberFormat.documentNumber(tx.id),
                details = listOf("التاريخ $date"),
                logo = data.logo,
                printedAt = data.printedAt
            ),
            data.options
        )
        builder.newPage()
        drawDetails(builder, data, date)
        drawItems(builder, data)
        drawTotalsAndPayment(builder, data)
        if (tx.note.isNotBlank()) PdfNoteCard.draw(builder, "ملاحظات", tx.note)
        drawSignature(builder)
        builder.renderTo(sink)
        return builder
    }

    private fun statusTone(tx: Transaction): PdfBadgeTone = when (tx.returnStatus) {
        TransactionReturnStatus.FULLY_RETURNED -> PdfBadgeTone.RETURNED
        TransactionReturnStatus.PARTIALLY_RETURNED -> PdfBadgeTone.PARTIAL
        else -> if (tx.isPaid) PdfBadgeTone.PAID else PdfBadgeTone.CREDIT
    }

    private fun drawDetails(builder: PdfDocumentBuilder, data: InvoicePdfData, date: String) {
        val tx = data.transaction
        val phone = data.customer?.phone.orEmpty()
        val tone = statusTone(tx)
        PdfInfoCard.draw(
            builder,
            listOf(
                "رقم الفاتورة" to AppNumberFormat.documentNumber(tx.id),
                "التاريخ" to date,
                "العميل" to (data.customer?.name ?: tx.customerName).ifBlank { "زبون زائر" },
                "الهاتف" to if (phone.isBlank()) "—" else AppNumberFormat.isolate(phone),
                "طريقة الدفع" to paymentTypeLabel(tx.paymentType, tx.paymentMethodName),
                "الحالة" to ""
            ),
            badge = PdfStatusBadge.label(tone) to tone
        )
    }

    private fun drawItems(builder: PdfDocumentBuilder, data: InvoicePdfData) {
        PdfSectionTitle.draw(builder, "تفاصيل الأصناف")
        val table = PdfTable(
            builder,
            listOf(
                PdfColumn("#", 0.45f),
                PdfColumn("الصنف", 3.2f, Layout.Alignment.ALIGN_NORMAL),
                PdfColumn("الوحدة", 1.0f),
                PdfColumn("الكمية", 1.0f),
                PdfColumn("السعر", 1.35f),
                PdfColumn("الإجمالي", 1.5f)
            ),
            continuationTitle = "تفاصيل الأصناف"
        )
        table.start()
        if (data.items.isEmpty()) {
            table.message("فاتورة بمبلغ يدوي دون أصناف مسجلة")
        } else {
            data.items.forEachIndexed { index, item ->
                val returned = data.returnSummary.returnedByUnit[item.unitId] ?: 0.0
                val net = (item.quantity - returned).coerceAtLeast(0.0)
                val name = (if (returned > 0.0) "${item.productName} (بعد الإرجاع)" else item.productName).ifBlank { "—" }
                table.row(
                    listOf(
                        PdfCell(AppNumberFormat.integer(index + 1), color = builder.palette.textMuted),
                        PdfCell(name, PdfTypeScale.tableCellStrong, builder.palette.textPrimary),
                        PdfCell(item.unitLabel.ifBlank { "—" }),
                        PdfCell(AppNumberFormat.quantity(net)),
                        PdfCell(AppNumberFormat.money(item.pricePerUnit)),
                        PdfCell(AppNumberFormat.money(net * item.pricePerUnit), PdfTypeScale.tableCellStrong, builder.palette.brand)
                    )
                )
            }
        }
        table.finish()
    }

    private fun drawTotalsAndPayment(builder: PdfDocumentBuilder, data: InvoicePdfData) {
        val tx = data.transaction
        val returned = data.returnSummary.totalRefunded.coerceAtLeast(0.0)
        val totals = buildList {
            add(PdfSummaryRow("المجموع", AppNumberFormat.money(tx.amount + returned)))
            if (returned > 0.0) add(PdfSummaryRow("المرتجع", AppNumberFormat.deduction(returned)))
            add(PdfSummaryRow("الإجمالي", AppNumberFormat.money(tx.amount), emphasized = true))
        }
        val (paymentTitle, payment) = if (tx.paymentType == PaymentType.DEBT) {
            "تفاصيل البيع الآجل" to buildList {
                data.priorDebtBalance?.let { add(PdfSummaryRow("رصيده قبل الفاتورة", AppNumberFormat.money(it))) }
                add(PdfSummaryRow("صافي الفاتورة", AppNumberFormat.money(tx.amount)))
                tx.dueDate?.let { add(PdfSummaryRow("تاريخ الاستحقاق", AppNumberFormat.date(it))) }
                add(PdfSummaryRow("حالة الدين", if (tx.isPaid) "مسدد" else "غير مسدد",
                    tone = if (tx.isPaid) PdfBadgeTone.PAID else PdfBadgeTone.CREDIT))
                add(PdfSummaryRow("المتبقي", AppNumberFormat.money(if (tx.isPaid) 0.0 else tx.amount), emphasized = true))
                data.currentDebtBalance?.let { add(PdfSummaryRow("رصيد الحساب الحالي", AppNumberFormat.money(it), emphasized = true)) }
            }
        } else {
            val paid = if (tx.isPaid) tx.amount else 0.0
            "تفاصيل الدفع" to listOf(
                PdfSummaryRow("طريقة الدفع", paymentTypeLabel(tx.paymentType, tx.paymentMethodName)),
                PdfSummaryRow("المدفوع", AppNumberFormat.money(paid)),
                PdfSummaryRow("المتبقي", AppNumberFormat.money((tx.amount - paid).coerceAtLeast(0.0)), emphasized = true)
            )
        }
        // Totals on the left, payment on the right (RTL reading order), never split across pages.
        val columnWidth = (builder.width - PdfSpacing.MD) / 2f
        val height = max(
            PdfSummaryCard.height(builder, "الإجماليات", totals, columnWidth),
            PdfSummaryCard.height(builder, paymentTitle, payment, columnWidth)
        )
        builder.ensureSpace(height)
        val top = builder.y
        PdfSummaryCard.draw(builder, paymentTitle, payment, left = builder.right - columnWidth, width = columnWidth)
        builder.y = top
        PdfSummaryCard.draw(builder, "الإجماليات", totals, left = builder.left, width = columnWidth,
            background = builder.palette.successContainer)
        builder.y = top + height + PdfSpacing.LG
    }

    private fun drawSignature(builder: PdfDocumentBuilder) {
        val height = 48f
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        val palette = builder.palette
        val signatureWidth = 180f
        builder.text.text(canvas, "التوقيع", builder.right - signatureWidth, top, signatureWidth, PdfTypeScale.label,
            palette.textMuted)
        canvas.drawLine(builder.right - signatureWidth, top + 34f, builder.right, top + 34f, builder.paints.stroke(palette.border, 1f))
        builder.text.text(canvas, "شكراً لتعاملكم معنا", builder.left, top + 12f, 220f, PdfTypeScale.bodyStrong, palette.brand,
            Layout.Alignment.ALIGN_OPPOSITE)
        builder.log("signature", top, top + height)
        builder.y = top + height
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
