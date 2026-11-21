package com.trader.salesmanager.util.pdf.components

import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Two-column key/value card for document details (invoice number, date, customer, ...). */
object PdfInfoCard {
    private const val ROW_HEIGHT = 22f

    fun draw(builder: PdfDocumentBuilder, entries: List<Pair<String, String>>, badge: Pair<String, PdfBadgeTone>? = null) {
        val palette = builder.palette
        val rows = (entries.size + 1) / 2
        val height = PdfSpacing.CARD_PADDING * 2 + rows * ROW_HEIGHT
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.fill(palette.surfaceAlt))
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        val columnWidth = (builder.width - PdfSpacing.CARD_PADDING * 3) / 2f
        entries.forEachIndexed { index, (label, value) ->
            val column = index % 2
            val row = index / 2
            val right = builder.right - PdfSpacing.CARD_PADDING - column * (columnWidth + PdfSpacing.CARD_PADDING)
            val rowTop = top + PdfSpacing.CARD_PADDING + row * ROW_HEIGHT
            val labelWidth = 82f
            val labelLayout = builder.text.layout("$label:", labelWidth, PdfTypeScale.label, palette.textMuted, maxLines = 1)
            builder.text.draw(canvas, labelLayout, right - labelWidth, rowTop + (ROW_HEIGHT - labelLayout.height) / 2f)
            val valueLayout = builder.text.layout(value, columnWidth - labelWidth - PdfSpacing.SM, PdfTypeScale.bodyStrong,
                palette.textPrimary, Layout.Alignment.ALIGN_NORMAL, 1)
            builder.text.draw(canvas, valueLayout, right - columnWidth, rowTop + (ROW_HEIGHT - valueLayout.height) / 2f)
        }
        badge?.let { (text, tone) ->
            val badgeWidth = builder.text.paint(PdfTypeScale.badge, 0).measureText(text) + PdfSpacing.MD * 2
            PdfStatusBadge.draw(canvas, builder, text, tone, builder.left + PdfSpacing.CARD_PADDING + badgeWidth / 2f,
                top + height - PdfSpacing.CARD_PADDING - ROW_HEIGHT + (ROW_HEIGHT - PdfStatusBadge.HEIGHT) / 2f)
        }
        builder.log("info-card", top, top + height)
        builder.y = top + height + PdfSpacing.LG
    }
}
