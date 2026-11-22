package com.trader.salesmanager.util.pdf

import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.Transaction
import com.trader.core.util.format.AppNumberFormat
import com.trader.salesmanager.util.pdf.components.PdfBadgeTone
import com.trader.salesmanager.util.pdf.components.PdfCell
import com.trader.salesmanager.util.pdf.components.PdfColumn
import com.trader.salesmanager.util.pdf.components.PdfInfoCard
import com.trader.salesmanager.util.pdf.components.PdfMetricCard
import com.trader.salesmanager.util.pdf.components.PdfSectionTitle
import com.trader.salesmanager.util.pdf.components.PdfTable
import com.trader.salesmanager.util.pdf.core.AndroidPdfPageSink
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.core.PdfHeaderContent
import com.trader.salesmanager.util.pdf.core.PdfPageSink
import com.trader.salesmanager.util.pdf.design.PdfOptions
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale
import java.io.File

/**
 * Customer account statement. Every transaction is printed: the table paginates with a
 * repeated header (the previous single-page version stopped after 25 rows).
 */
object CustomerStatementPdfGenerator {
    data class StatementPdfData(
        val customer: Customer,
        val transactions: List<Transaction>,
        val storeName: String,
        val options: PdfOptions = PdfOptions.DEFAULT,
        val printedAt: Long = System.currentTimeMillis()
    )

    fun generate(cacheDir: File, customer: Customer, transactions: List<Transaction>, storeName: String,
                 options: PdfOptions = PdfOptions.DEFAULT): File {
        val sink = AndroidPdfPageSink()
        try {
            render(StatementPdfData(customer, transactions, storeName, options), sink)
            return sink.writeTo(File(cacheDir, "statement_${customer.id}.pdf"))
        } finally {
            sink.close()
        }
    }

    fun render(data: StatementPdfData, sink: PdfPageSink): PdfDocumentBuilder {
        val customer = data.customer
        val builder = PdfDocumentBuilder(
            PdfHeaderContent(
                storeName = data.storeName.ifBlank { "المتجر" },
                documentTitle = "كشف حساب",
                documentNumber = null,
                details = listOf(customer.name),
                printedAt = data.printedAt
            ),
            data.options
        )
        val palette = builder.palette
        val total = data.transactions.sumOf { it.amount }
        val paid = data.transactions.filter { it.isPaid }.sumOf { it.amount }
        val unpaid = total - paid
        builder.newPage()
        val dates = data.transactions.map { it.date }
        PdfInfoCard.draw(
            builder,
            listOf(
                "العميل" to customer.name.ifBlank { "—" },
                "الهاتف" to if (customer.phone.isBlank()) "—" else AppNumberFormat.isolate(customer.phone),
                "عدد العمليات" to AppNumberFormat.integer(data.transactions.size),
                "الفترة" to if (dates.isEmpty()) "—" else
                    "${AppNumberFormat.date(dates.min())} – ${AppNumberFormat.date(dates.max())}"
            )
        )
        PdfMetricCard.grid(
            builder,
            listOf(
                Triple("الإجمالي", AppNumberFormat.money(total), palette.textPrimary),
                Triple("المدفوع", AppNumberFormat.money(paid), palette.success),
                Triple("الدين المتبقي", AppNumberFormat.money(unpaid), palette.danger)
            ),
            perRow = 3
        )
        builder.gap(PdfSpacing.SM)
        PdfSectionTitle.draw(builder, "سجل العمليات")
        val table = PdfTable(
            builder,
            listOf(
                PdfColumn("رقم العملية", 1.1f),
                PdfColumn("التاريخ", 1.2f),
                PdfColumn("طريقة الدفع", 1.6f),
                PdfColumn("الحالة", 1.0f),
                PdfColumn("المبلغ", 1.4f)
            ),
            continuationTitle = "سجل العمليات"
        )
        table.start()
        if (data.transactions.isEmpty()) {
            table.message("لا توجد عمليات لهذا العميل")
        } else {
            data.transactions.forEach { tx ->
                val tone = if (tx.isPaid) PdfBadgeTone.PAID else PdfBadgeTone.CREDIT
                table.row(
                    listOf(
                        PdfCell(AppNumberFormat.reference(tx.id), color = palette.textMuted),
                        PdfCell(AppNumberFormat.date(tx.date)),
                        PdfCell(tx.paymentMethodName.ifBlank { "—" }),
                        PdfCell(if (tx.isPaid) "مدفوع" else "دين", badge = tone),
                        PdfCell(AppNumberFormat.money(tx.amount), PdfTypeScale.tableCellStrong, palette.textPrimary)
                    )
                )
            }
        }
        table.finish()
        builder.renderTo(sink)
        return builder
    }
}
