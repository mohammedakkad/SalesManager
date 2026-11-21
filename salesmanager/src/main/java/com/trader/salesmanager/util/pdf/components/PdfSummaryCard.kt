package com.trader.salesmanager.util.pdf.components

import android.text.Layout
import android.text.StaticLayout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Totals / summary card: title, label-value rows, emphasized rows after a divider. Never split across pages. */
object PdfSummaryCard {
    private const val ROW_HEIGHT = 20f
    private const val EMPHASIZED_ROW_HEIGHT = 26f

    private class Measured(val label: StaticLayout, val value: StaticLayout?, val height: Float)

    /** Values wrap to at most three lines instead of being ellipsized; each row is measured once. */
    private fun measure(builder: PdfDocumentBuilder, rows: List<PdfSummaryRow>, innerWidth: Float, accent: Int): List<Measured> {
        val palette = builder.palette
        val labelWidth = innerWidth * 0.5f
        return rows.map { row ->
            val base = if (row.emphasized) EMPHASIZED_ROW_HEIGHT else ROW_HEIGHT
            val label = builder.text.layout(row.label, labelWidth, if (row.emphasized) PdfTypeScale.bodyStrong else PdfTypeScale.body,
                palette.textSecondary, maxLines = 2)
            val value = if (row.tone != null) null else builder.text.layout(row.value, innerWidth - labelWidth,
                if (row.emphasized) PdfTypeScale.total else PdfTypeScale.bodyStrong,
                if (row.emphasized) accent else palette.textPrimary, Layout.Alignment.ALIGN_OPPOSITE, 3)
            Measured(label, value, maxOf(base, label.height + 6f, (value?.height ?: 0) + 6f))
        }
    }

    fun height(builder: PdfDocumentBuilder, title: String?, rows: List<PdfSummaryRow>, width: Float = builder.width,
               accent: Int = builder.palette.brand): Float {
        val titleHeight = title?.let { builder.text.height(it, 200f, PdfTypeScale.section, 1) + PdfSpacing.SM } ?: 0f
        return PdfSpacing.CARD_PADDING * 2 + titleHeight +
            measure(builder, rows, width - PdfSpacing.CARD_PADDING * 2, accent).sumOf { it.height.toDouble() }.toFloat()
    }

    /** Draws at the builder cursor within [left]..[left]+[width]; returns the card bottom. */
    fun draw(
        builder: PdfDocumentBuilder,
        title: String?,
        rows: List<PdfSummaryRow>,
        left: Float = builder.left,
        width: Float = builder.width,
        background: Int = builder.palette.surfaceAlt,
        accent: Int = builder.palette.brand
    ): Float {
        val palette = builder.palette
        val innerLeft = left + PdfSpacing.CARD_PADDING
        val innerWidth = width - PdfSpacing.CARD_PADDING * 2
        val measured = measure(builder, rows, innerWidth, accent)
        val titleHeight = title?.let { builder.text.height(it, 200f, PdfTypeScale.section, 1) + PdfSpacing.SM } ?: 0f
        val height = PdfSpacing.CARD_PADDING * 2 + titleHeight + measured.sumOf { it.height.toDouble() }.toFloat()
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        canvas.drawRoundRect(left, top, left + width, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.fill(background))
        canvas.drawRoundRect(left, top, left + width, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        var y = top + PdfSpacing.CARD_PADDING
        title?.let {
            builder.text.text(canvas, it, innerLeft, y, innerWidth, PdfTypeScale.section, palette.textPrimary, maxLines = 1)
            y += titleHeight
        }
        var dividerDrawn = false
        val labelWidth = innerWidth * 0.5f
        rows.forEachIndexed { i, row ->
            val m = measured[i]
            if (row.emphasized && !dividerDrawn) {
                canvas.drawLine(innerLeft, y, innerLeft + innerWidth, y, builder.paints.stroke(palette.divider, 0.75f))
                dividerDrawn = true
            }
            builder.text.draw(canvas, m.label, innerLeft + innerWidth - labelWidth, y + (m.height - m.label.height) / 2f)
            if (row.tone != null) {
                val badgeWidth = builder.text.paint(PdfTypeScale.badge, 0).measureText(row.value) + PdfSpacing.MD * 2
                PdfStatusBadge.draw(canvas, builder, row.value, row.tone, innerLeft + badgeWidth / 2f,
                    y + (m.height - PdfStatusBadge.HEIGHT) / 2f)
            } else if (m.value != null) {
                builder.text.draw(canvas, m.value, innerLeft, y + (m.height - m.value.height) / 2f)
            }
            y += m.height
        }
        builder.log("summary-card", top, top + height)
        return top + height
    }
}
