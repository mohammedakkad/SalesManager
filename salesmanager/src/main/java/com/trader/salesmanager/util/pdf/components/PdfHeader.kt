package com.trader.salesmanager.util.pdf.components

import android.graphics.Canvas
import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Consistent page header: full header on page 1, compact "continued" header afterwards. */
class PdfHeader(private val builder: PdfDocumentBuilder) {
    private val palette = builder.palette
    private val content = builder.header

    fun draw(canvas: Canvas, continuation: Boolean) {
        val height = if (continuation) PdfSpacing.CONTINUATION_HEADER_HEIGHT else PdfSpacing.HEADER_HEIGHT
        canvas.drawRect(0f, 0f, PdfSpacing.PAGE_WIDTH.toFloat(), height, builder.paints.fill(palette.headerBand))
        if (palette.headerBand == android.graphics.Color.TRANSPARENT) {
            canvas.drawLine(PdfSpacing.MARGIN, height - 1f, PdfSpacing.PAGE_WIDTH - PdfSpacing.MARGIN, height - 1f,
                builder.paints.stroke(palette.border, 1f))
        }
        val right = PdfSpacing.PAGE_WIDTH - PdfSpacing.MARGIN
        val logoSize = if (continuation) 0f else PdfLogo.SIZE
        if (!continuation) PdfLogo.draw(canvas, builder, right - logoSize, (height - logoSize) / 2f, content.logo, content.storeName)
        val titleRight = right - if (continuation) 0f else logoSize + PdfSpacing.MD
        val titleWidth = titleRight - PdfSpacing.PAGE_WIDTH / 2f + PdfSpacing.LG
        val text = builder.text
        val nameStyle = if (continuation) PdfTypeScale.title else PdfTypeScale.display
        val nameTop = if (continuation) PdfSpacing.MD else PdfSpacing.XL
        val nameHeight = text.text(canvas, content.storeName, titleRight - titleWidth, nameTop, titleWidth,
            nameStyle, palette.headerText, maxLines = 1)
        val title = if (continuation) "${content.documentTitle} — تابع" else content.documentTitle
        text.text(canvas, title, titleRight - titleWidth, nameTop + nameHeight + 2f, titleWidth,
            PdfTypeScale.body, palette.headerTextMuted, maxLines = 1)

        // Left side (opposite in RTL): document number and short details.
        val leftWidth = PdfSpacing.PAGE_WIDTH / 2f - PdfSpacing.MARGIN - PdfSpacing.LG
        var top = if (continuation) PdfSpacing.MD + 2f else PdfSpacing.XL
        content.documentNumber?.let {
            top += text.text(canvas, it, PdfSpacing.MARGIN, top, leftWidth, PdfTypeScale.title, palette.headerText,
                Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1) + 2f
        }
        if (!continuation) {
            content.details.forEach { line ->
                top += text.text(canvas, line, PdfSpacing.MARGIN, top, leftWidth, PdfTypeScale.caption,
                    palette.headerTextMuted, Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1) + 1f
            }
        }
    }

    companion object {
        fun contentTop(continuation: Boolean): Float =
            (if (continuation) PdfSpacing.CONTINUATION_HEADER_HEIGHT else PdfSpacing.HEADER_HEIGHT) + PdfSpacing.LG
    }
}
