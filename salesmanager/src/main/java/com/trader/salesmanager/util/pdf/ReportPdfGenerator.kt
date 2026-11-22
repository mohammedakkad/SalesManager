package com.trader.salesmanager.util.pdf

import android.text.Layout
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.util.format.AppNumberFormat
import com.trader.salesmanager.util.pdf.components.PdfCell
import com.trader.salesmanager.util.pdf.components.PdfColumn
import com.trader.salesmanager.util.pdf.components.PdfMetricCard
import com.trader.salesmanager.util.pdf.components.PdfSectionTitle
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
import java.util.Calendar

object ReportPdfGenerator {
    data class MonthlyReportPdfData(
        val storeName: String,
        val month: Int,
        val year: Int,
        val totalSales: Double,
        val netProfit: Double,
        val inventoryCostValue: Double,
        val inventorySaleValue: Double,
        val debtAging: DebtAging,
        val dailySalesProfit: List<DailySalesProfit>,
        val generatedAt: Long = System.currentTimeMillis(),
        val options: PdfOptions = PdfOptions.DEFAULT
    )

    fun generate(cacheDir: File, data: MonthlyReportPdfData): File {
        val sink = AndroidPdfPageSink()
        try {
            render(data, sink)
            val name = "monthly_report_${data.year}_${(data.month + 1).toString().padStart(2, '0')}.pdf"
            return sink.writeTo(File(cacheDir, name))
        } finally {
            sink.close()
        }
    }

    fun render(data: MonthlyReportPdfData, sink: PdfPageSink): PdfDocumentBuilder {
        val month = monthLabel(data.month, data.year)
        val builder = PdfDocumentBuilder(
            PdfHeaderContent(
                storeName = data.storeName.ifBlank { "المتجر" },
                documentTitle = "تقرير شهري",
                documentNumber = month,
                details = listOf("تاريخ الإنشاء ${AppNumberFormat.dateTime(data.generatedAt)}"),
                printedAt = data.generatedAt
            ),
            data.options
        )
        val palette = builder.palette
        builder.newPage()
        PdfMetricCard.grid(
            builder,
            listOf(
                Triple("إجمالي المبيعات", AppNumberFormat.money(data.totalSales), palette.success),
                Triple("إجمالي الأرباح", AppNumberFormat.money(data.netProfit), if (data.netProfit < 0) palette.danger else palette.info),
                Triple("قيمة المخزون", AppNumberFormat.money(data.inventorySaleValue), palette.accent),
                Triple("إجمالي الديون", AppNumberFormat.money(data.debtAging.totalAmount), palette.danger)
            )
        )
        builder.gap(PdfSpacing.SM)
        builder.y = PdfSummaryCard.draw(
            builder,
            "قيمة المخزون الحالية",
            listOf(
                PdfSummaryRow("قيمة التكلفة", AppNumberFormat.money(data.inventoryCostValue)),
                PdfSummaryRow("قيمة البيع", AppNumberFormat.money(data.inventorySaleValue))
            )
        ) + PdfSpacing.LG
        drawDebtAging(builder, data.debtAging)
        drawDaily(builder, data.dailySalesProfit)
        builder.renderTo(sink)
        return builder
    }

    private fun drawDebtAging(builder: PdfDocumentBuilder, debt: DebtAging) {
        val palette = builder.palette
        val rows = listOf(
            Triple("أقل من أسبوع", debt.lessThanWeekAmount to debt.lessThanWeekCount, palette.warning),
            Triple("من أسبوع إلى شهر", debt.oneWeekToOneMonthAmount to debt.oneWeekToOneMonthCount, palette.info),
            Triple("من شهر إلى ٣ أشهر", debt.oneMonthToThreeMonthsAmount to debt.oneMonthToThreeMonthsCount, palette.accent),
            Triple("أكثر من ٣ أشهر", debt.moreThanThreeMonthsAmount to debt.moreThanThreeMonthsCount, palette.danger)
        )
        val rowHeight = 30f
        PdfSectionTitle.draw(builder, "أعمار الديون", keepWithNext = rows.size * rowHeight + PdfSpacing.MD)
        val top = builder.y
        val canvas = builder.canvas
        val height = rows.size * rowHeight + PdfSpacing.MD
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        val maxAmount = rows.maxOf { it.second.first }.coerceAtLeast(1.0)
        val inner = builder.width - PdfSpacing.CARD_PADDING * 2
        rows.forEachIndexed { index, (label, value, color) ->
            val rowTop = top + PdfSpacing.SM + index * rowHeight
            val left = builder.left + PdfSpacing.CARD_PADDING
            builder.text.text(canvas, label, left + inner / 2f, rowTop, inner / 2f, PdfTypeScale.label, palette.textSecondary, maxLines = 1)
            builder.text.text(canvas, "${AppNumberFormat.money(value.first)} · ${AppNumberFormat.integer(value.second)} فاتورة",
                left, rowTop, inner / 2f, PdfTypeScale.label, color, Layout.Alignment.ALIGN_OPPOSITE, 1)
            val barTop = rowTop + 15f
            canvas.drawRoundRect(left, barTop, left + inner, barTop + 6f, 3f, 3f, builder.paints.fill(palette.surfaceAlt))
            val fraction = (value.first / maxAmount).toFloat().coerceIn(0f, 1f)
            // Bars grow from the right (reading start) in an RTL document.
            canvas.drawRoundRect(left + inner * (1f - fraction), barTop, left + inner, barTop + 6f, 3f, 3f, builder.paints.fill(color))
        }
        builder.log("debt-aging", top, top + height)
        builder.y = top + height + PdfSpacing.LG
    }

    private fun drawDaily(builder: PdfDocumentBuilder, days: List<DailySalesProfit>) {
        val palette = builder.palette
        PdfSectionTitle.draw(builder, "المبيعات والأرباح اليومية")
        val table = PdfTable(
            builder,
            listOf(
                PdfColumn("اليوم", 0.7f),
                PdfColumn("التاريخ", 1.3f),
                PdfColumn("المبيعات", 1.5f),
                PdfColumn("صافي الربح", 1.5f)
            ),
            continuationTitle = "المبيعات والأرباح اليومية"
        )
        table.start()
        if (days.isEmpty()) {
            table.message("لا توجد مبيعات مسجلة خلال هذا الشهر")
        } else {
            days.forEach { row ->
                val day = Calendar.getInstance().apply { timeInMillis = row.dayStartMillis }.get(Calendar.DAY_OF_MONTH)
                table.row(
                    listOf(
                        PdfCell(AppNumberFormat.integer(day), PdfTypeScale.tableCellStrong, palette.textPrimary),
                        PdfCell(AppNumberFormat.date(row.dayStartMillis)),
                        PdfCell(AppNumberFormat.money(row.sales), PdfTypeScale.tableCellStrong, palette.info),
                        PdfCell(AppNumberFormat.money(row.profit), PdfTypeScale.tableCellStrong,
                            if (row.profit >= 0.0) palette.success else palette.danger)
                    )
                )
            }
        }
        table.finish()
    }

    private fun monthLabel(month: Int, year: Int): String {
        val months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        return "${months.getOrElse(month) { AppNumberFormat.integer(month + 1) }} ${AppNumberFormat.integer(year)}"
    }
}
