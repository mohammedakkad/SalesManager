package com.trader.salesmanager.util.pdf.components

import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Totals / summary card: title, label-value rows, emphasized rows after a divider. Never split across pages. */
object PdfSummaryCard {
    private const val ROW_HEIGHT = 20f
    private const val EMPHASIZED_ROW_HEIGHT = 26f

    fun height(builder: PdfDocumentBuilder, title: String?, rows: List<PdfSummaryRow>): Float {
        val titleHeight = title?.let { builder.text.height(it, 200f, PdfTypeScale.section, 1) + PdfSpacing.SM } ?: 0f
        return PdfSpacing.CARD_PADDING * 2 + titleHeight +
            rows.sumOf { (if (it.emphasized) EMPHASIZED_ROW_HEIGHT else ROW_HEIGHT).toDouble() }.toFloat()
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
        val height = height(builder, title, rows)
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        canvas.drawRoundRect(left, top, left + width, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.fill(background))
        canvas.drawRoundRect(left, top, left + width, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        val innerLeft = left + PdfSpacing.CARD_PADDING
        val innerWidth = width - PdfSpacing.CARD_PADDING * 2
        var y = top + PdfSpacing.CARD_PADDING
        title?.let {
            y += builder.text.text(canvas, it, innerLeft, y, innerWidth, PdfTypeScale.section, palette.textPrimary, maxLines = 1) +
                PdfSpacing.SM
        }
        var dividerDrawn = false
        rows.forEach { row ->
            val rowHeight = if (row.emphasized) EMPHASIZED_ROW_HEIGHT else ROW_HEIGHT
            if (row.emphasized && !dividerDrawn) {
                canvas.drawLine(innerLeft, y, innerLeft + innerWidth, y, builder.paints.stroke(palette.divider, 0.75f))
                dividerDrawn = true
            }
            val labelStyle = if (row.emphasized) PdfTypeScale.bodyStrong else PdfTypeScale.body
            val valueStyle = if (row.emphasized) PdfTypeScale.total else PdfTypeScale.bodyStrong
            val labelWidth = innerWidth * 0.5f
            val label = builder.text.layout(row.label, labelWidth, labelStyle, palette.textSecondary, maxLines = 1)
            builder.text.draw(canvas, label, innerLeft + innerWidth - labelWidth, y + (rowHeight - label.height) / 2f)
            if (row.tone != null) {
                val badgeWidth = builder.text.paint(PdfTypeScale.badge, 0).measureText(row.value) + PdfSpacing.MD * 2
                PdfStatusBadge.draw(canvas, builder, row.value, row.tone, innerLeft + badgeWidth / 2f,
                    y + (rowHeight - PdfStatusBadge.HEIGHT) / 2f)
            } else {
                val valueColor = if (row.emphasized) accent else palette.textPrimary
                val value = builder.text.layout(row.value, innerWidth - labelWidth, valueStyle, valueColor,
                    Layout.Alignment.ALIGN_OPPOSITE, 1)
                builder.text.draw(canvas, value, innerLeft, y + (rowHeight - value.height) / 2f)
            }
            y += rowHeight
        }
        builder.log("summary-card", top, top + height)
        return top + height
    }
}
