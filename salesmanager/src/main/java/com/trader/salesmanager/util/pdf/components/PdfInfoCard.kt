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
        val columnWidth = (builder.width - PdfSpacing.CARD_PADDING * 3) / 2f
        val labelWidth = 82f
        // Measure once: values wrap (never ellipsize) and each row takes the height of its taller cell.
        val labels = entries.map { builder.text.layout("${it.first}:", labelWidth, PdfTypeScale.label, palette.textMuted, maxLines = 1) }
        val values = entries.map {
            builder.text.layout(it.second, columnWidth - labelWidth - PdfSpacing.SM, PdfTypeScale.bodyStrong,
                palette.textPrimary, Layout.Alignment.ALIGN_NORMAL, 3)
        }
        val rowHeights = entries.indices.chunked(2).map { row -> maxOf(ROW_HEIGHT, row.maxOf { values[it].height + 6f }) }
        val height = PdfSpacing.CARD_PADDING * 2 + rowHeights.sum()
        builder.ensureSpace(height)
        val top = builder.y
        val canvas = builder.canvas
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.fill(palette.surfaceAlt))
        canvas.drawRoundRect(builder.left, top, builder.right, top + height, PdfSpacing.CARD_RADIUS, PdfSpacing.CARD_RADIUS,
            builder.paints.stroke(palette.border, 0.75f))
        var rowTop = top + PdfSpacing.CARD_PADDING
        rowHeights.forEachIndexed { row, rowHeight ->
            for (column in 0..1) {
                val index = row * 2 + column
                if (index >= entries.size) break
                val right = builder.right - PdfSpacing.CARD_PADDING - column * (columnWidth + PdfSpacing.CARD_PADDING)
                builder.text.draw(canvas, labels[index], right - labelWidth, rowTop + (rowHeight - labels[index].height) / 2f)
                builder.text.draw(canvas, values[index], right - columnWidth, rowTop + (rowHeight - values[index].height) / 2f)
            }
            rowTop += rowHeight
        }
        badge?.let { (text, tone) ->
            val badgeWidth = builder.text.paint(PdfTypeScale.badge, 0).measureText(text) + PdfSpacing.MD * 2
            val lastHeight = rowHeights.last()
            PdfStatusBadge.draw(canvas, builder, text, tone, builder.left + PdfSpacing.CARD_PADDING + badgeWidth / 2f,
                top + height - PdfSpacing.CARD_PADDING - lastHeight + (lastHeight - PdfStatusBadge.HEIGHT) / 2f)
        }
        builder.log("info-card", top, top + height)
        builder.y = top + height + PdfSpacing.LG
    }
}
