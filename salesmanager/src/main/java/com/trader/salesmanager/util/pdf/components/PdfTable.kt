package com.trader.salesmanager.util.pdf.components

import android.text.StaticLayout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale
import kotlin.math.max

/**
 * Paginating table: zebra rows, header repeated on every page, rows never split or clipped.
 * Each row is measured once (one StaticLayout per cell) and the same layouts are drawn.
 */
class PdfTable(
    private val builder: PdfDocumentBuilder,
    private val columns: List<PdfColumn>,
    private val continuationTitle: String? = null
) {
    private val palette = builder.palette
    private val widths: List<Float> = columns.sumOf { it.weight.toDouble() }.toFloat().let { total ->
        columns.map { builder.width * it.weight / total }
    }
    private var rowIndex = 0

    /** Starts the table, keeping the header together with at least one row. */
    fun start() {
        builder.ensureSpace(PdfSpacing.TABLE_HEADER_HEIGHT + PdfSpacing.TABLE_ROW_MIN_HEIGHT)
        drawHeader()
    }

    fun row(cells: List<PdfCell>) {
        require(cells.size == columns.size) { "Expected ${columns.size} cells, got ${cells.size}" }
        val layouts: List<StaticLayout?> = cells.mapIndexed { i, cell ->
            if (cell.badge != null) null else builder.text.layout(
                cell.text, widths[i] - PdfSpacing.CELL_PADDING * 2, cell.style,
                cell.color ?: palette.textSecondary, columns[i].alignment, cell.maxLines
            )
        }
        val contentHeight = layouts.maxOfOrNull { it?.height?.toFloat() ?: PdfStatusBadge.HEIGHT } ?: 0f
        val height = max(PdfSpacing.TABLE_ROW_MIN_HEIGHT, contentHeight + PdfSpacing.CELL_PADDING * 2)
        builder.ensureSpace(height) { repeatHeader() }
        val top = builder.y
        val canvas = builder.canvas
        val background = if (rowIndex % 2 == 0) palette.surface else palette.surfaceAlt
        canvas.drawRect(builder.left, top, builder.right, top + height, builder.paints.fill(background))
        canvas.drawLine(builder.left, top + height, builder.right, top + height, builder.paints.stroke(palette.divider, 0.5f))
        var cellRight = builder.right
        cells.forEachIndexed { i, cell ->
            val cellLeft = cellRight - widths[i]
            val layout = layouts[i]
            if (layout != null) {
                builder.text.draw(canvas, layout, cellLeft + PdfSpacing.CELL_PADDING, top + (height - layout.height) / 2f)
            } else if (cell.badge != null) {
                PdfStatusBadge.draw(canvas, builder, cell.text, cell.badge, cellLeft + widths[i] / 2f,
                    top + (height - PdfStatusBadge.HEIGHT) / 2f)
            }
            cellRight = cellLeft
        }
        builder.log("table-row", top, top + height)
        builder.y = top + height
        rowIndex++
    }

    /** Explanatory full-width row (e.g. "no items"). */
    fun message(text: String) {
        val layout = builder.text.layout(text, builder.width - PdfSpacing.LG * 2, PdfTypeScale.body, palette.textMuted,
            android.text.Layout.Alignment.ALIGN_CENTER)
        val height = layout.height + PdfSpacing.LG * 2
        builder.ensureSpace(height) { repeatHeader() }
        val top = builder.y
        builder.canvas.drawRect(builder.left, top, builder.right, top + height, builder.paints.fill(palette.surfaceAlt))
        builder.text.draw(builder.canvas, layout, builder.left + PdfSpacing.LG, top + PdfSpacing.LG)
        builder.log("table-message", top, top + height)
        builder.y = top + height
    }

    /** Closes the table with an outline around the rows drawn on the current page. */
    fun finish() {
        builder.gap(PdfSpacing.LG)
    }

    private fun repeatHeader() {
        continuationTitle?.let { PdfSectionTitle.draw(builder, "$it — تابع", keepWithNext = 0f) }
        drawHeader()
    }

    private fun drawHeader() {
        val top = builder.y
        val height = PdfSpacing.TABLE_HEADER_HEIGHT
        val canvas = builder.canvas
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, 4f, 4f, builder.paints.fill(palette.tableHeader))
        if (palette.tableHeader == palette.surfaceAlt || palette.onTableHeader == palette.textPrimary) {
            canvas.drawRoundRect(builder.left, top, builder.right, top + height, 4f, 4f, builder.paints.stroke(palette.border, 0.75f))
        }
        var cellRight = builder.right
        columns.forEachIndexed { i, column ->
            val cellLeft = cellRight - widths[i]
            val layout = builder.text.layout(column.title, widths[i] - PdfSpacing.CELL_PADDING * 2, PdfTypeScale.tableHeader,
                palette.onTableHeader, column.alignment, 1)
            builder.text.draw(canvas, layout, cellLeft + PdfSpacing.CELL_PADDING, top + (height - layout.height) / 2f)
            cellRight = cellLeft
        }
        builder.log("table-header", top, top + height)
        builder.y = top + height
        rowIndex = 0
    }
}
