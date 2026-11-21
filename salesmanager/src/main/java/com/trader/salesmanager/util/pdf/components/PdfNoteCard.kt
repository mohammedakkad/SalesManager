package com.trader.salesmanager.util.pdf.components

import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Titled free-text card (notes). Long notes are limited to [maxLines] with an ellipsis. */
object PdfNoteCard {
    fun draw(builder: PdfDocumentBuilder, title: String, body: String, maxLines: Int = 8) {
        val palette = builder.palette
        val inner = builder.width - PdfSpacing.CARD_PADDING * 2
        val titleLayout = builder.text.layout(title, inner, PdfTypeScale.section, palette.textPrimary, maxLines = 1)
        val bodyLayout = builder.text.layout(body, inner, PdfTypeScale.body, palette.textSecondary, maxLines = maxLines)
        val height = PdfSpacing.CARD_PADDING * 2 + titleLayout.height + PdfSpacing.XS + bodyLayout.height
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        builder.text.draw(canvas, titleLayout, builder.left + PdfSpacing.CARD_PADDING, top + PdfSpacing.CARD_PADDING)
        builder.text.draw(canvas, bodyLayout, builder.left + PdfSpacing.CARD_PADDING,
            top + PdfSpacing.CARD_PADDING + titleLayout.height + PdfSpacing.XS)
        builder.log("note-card", top, top + height)
        builder.y = top + height + PdfSpacing.LG
    }
}
