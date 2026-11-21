package com.trader.salesmanager.util.pdf

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.text.Layout
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.salesmanager.ui.inventory.invoice.formatAmount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

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
        val generatedAt: Long = System.currentTimeMillis()
    )

    fun generate(cacheDir: File, data: MonthlyReportPdfData): File {
        val document = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNumber = 0
        var y = 0f
        val storeName = data.storeName.ifBlank { "المتجر" }
        val monthLabel = monthLabel(data.month, data.year)

        fun finishPage() {
            page?.let(document::finishPage)
            page = null
            canvas = null
        }

        fun startPage(continuation: Boolean = pageNumber > 0) {
            finishPage()
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(
                    PdfLayoutConstants.PAGE_WIDTH,
                    PdfLayoutConstants.PAGE_HEIGHT,
                    pageNumber
                ).create()
            )
            canvas = page!!.canvas
            drawHeader(canvas!!, storeName, monthLabel, data.generatedAt, continuation)
            drawFooter(canvas!!, storeName, data.generatedAt, pageNumber)
            y = if (continuation) {
                PdfLayoutConstants.CONTINUATION_HEADER_HEIGHT + 14f
            } else {
                PdfLayoutConstants.HEADER_HEIGHT + 14f
            }
        }

        fun ensureSpace(height: Float) {
            if (y + height > PdfLayoutConstants.CONTENT_BOTTOM) startPage(true)
        }

        fun drawDailyHeader() {
            val target = canvas!!
            target.drawRect(
                PdfLayoutConstants.MARGIN,
                y,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                y + PdfLayoutConstants.TABLE_HEADER_HEIGHT,
                PdfCanvas.fill(PdfLayoutConstants.EMERALD_700)
            )
            val widths = listOf(128f, 150f, 150f, 95f)
            val labels = listOf("صافي الربح", "المبيعات", "التاريخ", "اليوم")
            var left = PdfLayoutConstants.MARGIN
            labels.forEachIndexed { index, label ->
                PdfCanvas.cellText(
                    target,
                    label,
                    left,
                    y,
                    widths[index],
                    PdfLayoutConstants.TABLE_HEADER_HEIGHT,
                    9f,
                    PdfLayoutConstants.WHITE,
                    bold = true
                )
                left += widths[index]
            }
            y += PdfLayoutConstants.TABLE_HEADER_HEIGHT
        }

        try {
            startPage(false)
            y = drawSummaryCards(canvas!!, y, data)
            y += 14f
            ensureSpace(108f)
            y = drawInventorySection(canvas!!, y, data)
            y += 14f
            ensureSpace(205f)
            y = drawDebtAgingSection(canvas!!, y, data.debtAging)
            y += 18f
            ensureSpace(70f)
            PdfCanvas.text(
                canvas!!,
                "المبيعات والأرباح اليومية",
                PdfLayoutConstants.MARGIN,
                y,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                13f,
                PdfLayoutConstants.SLATE_900,
                bold = true
            )
            y += 23f
            drawDailyHeader()

            if (data.dailySalesProfit.isEmpty()) {
                PdfCanvas.roundRect(
                    canvas!!,
                    PdfLayoutConstants.MARGIN,
                    y,
                    PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
                    y + 52f,
                    PdfLayoutConstants.SLATE_50,
                    borderColor = PdfLayoutConstants.SLATE_200
                )
                PdfCanvas.cellText(
                    canvas!!,
                    "لا توجد مبيعات مسجلة خلال هذا الشهر",
                    PdfLayoutConstants.MARGIN,
                    y,
                    PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                    52f,
                    10f,
                    PdfLayoutConstants.SLATE_600
                )
                y += 52f
            } else {
                val maxSales = data.dailySalesProfit.maxOf { it.sales }.coerceAtLeast(1.0)
                data.dailySalesProfit.forEachIndexed { index, row ->
                    val rowHeight = 35f
                    if (y + rowHeight > PdfLayoutConstants.CONTENT_BOTTOM) {
                        startPage(true)
                        PdfCanvas.text(
                            canvas!!,
                            "المبيعات والأرباح اليومية - تابع",
                            PdfLayoutConstants.MARGIN,
                            y,
                            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
                            12f,
                            PdfLayoutConstants.SLATE_900,
                            bold = true
                        )
                        y += 22f
                        drawDailyHeader()
                    }
                    drawDailyRow(canvas!!, y, rowHeight, row, index, maxSales)
                    y += rowHeight
                }
            }
            finishPage()
            val output = File(
                cacheDir,
                "monthly_report_${data.year}_${(data.month + 1).toString().padStart(2, '0')}.pdf"
            )
            FileOutputStream(output).use(document::writeTo)
            return output
        } finally {
            if (page != null) finishPage()
            document.close()
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        storeName: String,
        monthLabel: String,
        generatedAt: Long,
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
            PdfLayoutConstants.MARGIN,
            if (continuation) 15f else 18f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
            if (continuation) 17f else 21f,
            PdfLayoutConstants.WHITE,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            if (continuation) "تقرير شهري - تابع" else "تقرير شهري",
            PdfLayoutConstants.MARGIN,
            if (continuation) 40f else 50f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2,
            12f,
            PdfLayoutConstants.EMERALD_100,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            monthLabel,
            PdfLayoutConstants.MARGIN,
            if (continuation) 40f else 72f,
            200f,
            11f,
            PdfLayoutConstants.WHITE,
            bold = true,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
        if (!continuation) {
            val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
                .format(Date(generatedAt))
            PdfCanvas.text(
                canvas,
                "تاريخ الإنشاء $generated",
                PdfLayoutConstants.MARGIN,
                18f,
                220f,
                8f,
                PdfLayoutConstants.CYAN_100,
                alignment = Layout.Alignment.ALIGN_OPPOSITE
            )
        }
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        top: Float,
        data: MonthlyReportPdfData
    ): Float {
        val gap = 10f
        val cardWidth = (
            PdfLayoutConstants.PAGE_WIDTH -
                PdfLayoutConstants.MARGIN * 2 -
                gap
            ) / 2f
        val cardHeight = 70f
        metricCard(
            canvas,
            PdfLayoutConstants.MARGIN + cardWidth + gap,
            top,
            cardWidth,
            cardHeight,
            "إجمالي المبيعات",
            "₪ ${data.totalSales.formatAmount()}",
            PdfLayoutConstants.EMERALD_100,
            PdfLayoutConstants.EMERALD_700
        )
        metricCard(
            canvas,
            PdfLayoutConstants.MARGIN,
            top,
            cardWidth,
            cardHeight,
            "إجمالي الأرباح",
            "₪ ${data.netProfit.formatAmount()}",
            PdfLayoutConstants.CYAN_100,
            PdfLayoutConstants.CYAN_500
        )
        metricCard(
            canvas,
            PdfLayoutConstants.MARGIN + cardWidth + gap,
            top + cardHeight + gap,
            cardWidth,
            cardHeight,
            "قيمة المخزون",
            "₪ ${data.inventorySaleValue.formatAmount()}",
            PdfLayoutConstants.SLATE_100,
            PdfLayoutConstants.VIOLET_500
        )
        metricCard(
            canvas,
            PdfLayoutConstants.MARGIN,
            top + cardHeight + gap,
            cardWidth,
            cardHeight,
            "إجمالي الديون",
            "₪ ${data.debtAging.totalAmount.formatAmount()}",
            PdfLayoutConstants.RED_100,
            PdfLayoutConstants.RED_500
        )
        return top + cardHeight * 2f + gap
    }

    private fun metricCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        background: Int,
        accent: Int
    ) {
        PdfCanvas.roundRect(
            canvas,
            left,
            top,
            left + width,
            top + height,
            background,
            borderColor = PdfLayoutConstants.SLATE_200
        )
        canvas.drawRoundRect(
            left + width - 6f,
            top + 8f,
            left + width - 2f,
            top + height - 8f,
            2f,
            2f,
            PdfCanvas.fill(accent)
        )
        PdfCanvas.text(
            canvas,
            label,
            left + 14f,
            top + 13f,
            width - 28f,
            9.5f,
            PdfLayoutConstants.SLATE_600,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            value,
            left + 14f,
            top + 37f,
            width - 28f,
            15f,
            accent,
            bold = true
        )
    }

    private fun drawInventorySection(
        canvas: Canvas,
        top: Float,
        data: MonthlyReportPdfData
    ): Float {
        val height = 94f
        PdfCanvas.roundRect(
            canvas,
            PdfLayoutConstants.MARGIN,
            top,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
            top + height,
            PdfLayoutConstants.SLATE_50,
            borderColor = PdfLayoutConstants.SLATE_200
        )
        PdfCanvas.text(
            canvas,
            "قيمة المخزون الحالية",
            PdfLayoutConstants.MARGIN + 14f,
            top + 13f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 28f,
            12f,
            PdfLayoutConstants.SLATE_900,
            bold = true
        )
        drawValueLine(canvas, top + 42f, "قيمة التكلفة", data.inventoryCostValue, PdfLayoutConstants.AMBER_500)
        drawValueLine(canvas, top + 66f, "قيمة البيع", data.inventorySaleValue, PdfLayoutConstants.VIOLET_500)
        return top + height
    }

    private fun drawValueLine(
        canvas: Canvas,
        top: Float,
        label: String,
        value: Double,
        accent: Int
    ) {
        PdfCanvas.text(
            canvas,
            label,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 160f,
            top,
            145f,
            9.5f,
            PdfLayoutConstants.SLATE_700,
            bold = true
        )
        PdfCanvas.text(
            canvas,
            "₪ ${value.formatAmount()}",
            PdfLayoutConstants.MARGIN + 14f,
            top,
            200f,
            10.5f,
            accent,
            bold = true,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
    }

    private fun drawDebtAgingSection(
        canvas: Canvas,
        top: Float,
        debt: DebtAging
    ): Float {
        val rows = listOf(
            DebtRow("أقل من أسبوع", debt.lessThanWeekAmount, debt.lessThanWeekCount, PdfLayoutConstants.AMBER_500),
            DebtRow("من أسبوع إلى شهر", debt.oneWeekToOneMonthAmount, debt.oneWeekToOneMonthCount, PdfLayoutConstants.CYAN_500),
            DebtRow("من شهر إلى 3 أشهر", debt.oneMonthToThreeMonthsAmount, debt.oneMonthToThreeMonthsCount, PdfLayoutConstants.VIOLET_500),
            DebtRow("أكثر من 3 أشهر", debt.moreThanThreeMonthsAmount, debt.moreThanThreeMonthsCount, PdfLayoutConstants.RED_500)
        )
        val height = 190f
        PdfCanvas.roundRect(
            canvas,
            PdfLayoutConstants.MARGIN,
            top,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
            top + height,
            PdfLayoutConstants.WHITE,
            borderColor = PdfLayoutConstants.SLATE_200
        )
        PdfCanvas.text(
            canvas,
            "أعمار الديون",
            PdfLayoutConstants.MARGIN + 14f,
            top + 13f,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 28f,
            12f,
            PdfLayoutConstants.SLATE_900,
            bold = true
        )
        val maxAmount = rows.maxOf { it.amount }.coerceAtLeast(1.0)
        rows.forEachIndexed { index, row ->
            val rowTop = top + 43f + index * 35f
            PdfCanvas.text(
                canvas,
                row.label,
                PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN - 180f,
                rowTop,
                166f,
                8.8f,
                PdfLayoutConstants.SLATE_700,
                bold = true
            )
            PdfCanvas.text(
                canvas,
                "₪ ${row.amount.formatAmount()} · ${row.count}",
                PdfLayoutConstants.MARGIN + 14f,
                rowTop,
                180f,
                8.8f,
                row.color,
                bold = true,
                alignment = Layout.Alignment.ALIGN_OPPOSITE
            )
            val barLeft = PdfLayoutConstants.MARGIN + 14f
            val barTop = rowTop + 16f
            val barWidth = PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN * 2 - 28f
            canvas.drawRoundRect(
                barLeft,
                barTop,
                barLeft + barWidth,
                barTop + 6f,
                3f,
                3f,
                PdfCanvas.fill(PdfLayoutConstants.SLATE_100)
            )
            canvas.drawRoundRect(
                barLeft,
                barTop,
                barLeft + barWidth * (row.amount / maxAmount).toFloat().coerceIn(0f, 1f),
                barTop + 6f,
                3f,
                3f,
                PdfCanvas.fill(row.color)
            )
        }
        return top + height
    }

    private fun drawDailyRow(
        canvas: Canvas,
        top: Float,
        height: Float,
        row: DailySalesProfit,
        index: Int,
        maxSales: Double
    ) {
        val background = if (index % 2 == 0) {
            PdfLayoutConstants.WHITE
        } else {
            PdfLayoutConstants.SLATE_50
        }
        canvas.drawRect(
            PdfLayoutConstants.MARGIN,
            top,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
            top + height,
            PdfCanvas.fill(background)
        )
        canvas.drawRect(
            PdfLayoutConstants.MARGIN,
            top,
            PdfLayoutConstants.PAGE_WIDTH - PdfLayoutConstants.MARGIN,
            top + height,
            PdfCanvas.stroke(PdfLayoutConstants.SLATE_200, 0.5f)
        )
        val calendar = Calendar.getInstance().apply { timeInMillis = row.dayStartMillis }
        val values = listOf(
            "₪ ${row.profit.formatAmount()}",
            "₪ ${row.sales.formatAmount()}",
            SimpleDateFormat("dd/MM/yyyy", Locale("ar")).format(Date(row.dayStartMillis)),
            calendar.get(Calendar.DAY_OF_MONTH).toString()
        )
        val widths = listOf(128f, 150f, 150f, 95f)
        var left = PdfLayoutConstants.MARGIN
        values.forEachIndexed { column, value ->
            PdfCanvas.cellText(
                canvas,
                value,
                left,
                top,
                widths[column],
                height,
                8.8f,
                when (column) {
                    0 -> if (row.profit >= 0.0) PdfLayoutConstants.EMERALD_700 else PdfLayoutConstants.RED_500
                    1 -> PdfLayoutConstants.CYAN_500
                    else -> PdfLayoutConstants.SLATE_700
                },
                bold = column < 2
            )
            left += widths[column]
        }
        val salesBarWidth = 94f * (row.sales / maxSales).toFloat().coerceIn(0f, 1f)
        canvas.drawRect(
            PdfLayoutConstants.MARGIN + 132f,
            top + height - 4f,
            PdfLayoutConstants.MARGIN + 132f + salesBarWidth,
            top + height - 1f,
            PdfCanvas.fill(PdfLayoutConstants.CYAN_500)
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

    private fun monthLabel(month: Int, year: Int): String {
        val months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        return "${months.getOrElse(month) { (month + 1).toString() }} $year"
    }

    private data class DebtRow(
        val label: String,
        val amount: Double,
        val count: Int,
        val color: Int
    )
}
