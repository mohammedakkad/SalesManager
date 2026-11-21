package com.trader.salesmanager.util.pdf.components

import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Headline number card used in report/statement summaries (placed by the caller's grid). */
object PdfMetricCard {
    const val HEIGHT = 62f

    fun draw(builder: PdfDocumentBuilder, left: Float, top: Float, width: Float, label: String, value: String, accent: Int) {
        val palette = builder.palette
        val canvas = builder.canvas
        canvas.drawRoundRect(left, top, left + width, top + HEIGHT, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.fill(palette.surfaceAlt))
        canvas.drawRoundRect(left, top, left + width, top + HEIGHT, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        canvas.drawRoundRect(left + width - 4f, top + PdfSpacing.SM, left + width, top + HEIGHT - PdfSpacing.SM, 2f, 2f,
            builder.paints.fill(accent))
        val inner = width - PdfSpacing.CARD_PADDING * 2
        builder.text.text(canvas, label, left + PdfSpacing.CARD_PADDING, top + PdfSpacing.SM + 2f, inner,
            PdfTypeScale.label, palette.textMuted, maxLines = 1)
        builder.text.text(canvas, value, left + PdfSpacing.CARD_PADDING, top + PdfSpacing.SM + 22f, inner,
            PdfTypeScale.metric, accent, maxLines = 1)
    }

    /** Lays out [cards] (label, value, accent) in rows of [perRow] at the cursor; returns nothing, advances y. */
    fun grid(builder: PdfDocumentBuilder, cards: List<Triple<String, String, Int>>, perRow: Int = 2) {
        val gap = PdfSpacing.SM
        val width = (builder.width - gap * (perRow - 1)) / perRow
        cards.chunked(perRow).forEach { row ->
            builder.ensureSpace(HEIGHT)
            val top = builder.y
            row.forEachIndexed { i, (label, value, accent) ->
                val left = builder.right - (i + 1) * width - i * gap
                draw(builder, left, top, width, label, value, accent)
            }
            builder.log("metric-row", top, top + HEIGHT)
            builder.y = top + HEIGHT + gap
        }
    }
}
