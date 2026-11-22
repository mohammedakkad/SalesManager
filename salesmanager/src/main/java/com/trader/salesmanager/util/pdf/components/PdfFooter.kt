package com.trader.salesmanager.util.pdf.components

import android.graphics.Canvas
import android.text.Layout
import com.trader.core.util.format.AppNumberFormat
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Consistent footer: store name and "printed at · page X of Y". */
class PdfFooter(private val builder: PdfDocumentBuilder) {
    fun draw(canvas: Canvas, page: Int, total: Int) {
        val palette = builder.palette
        val top = PdfSpacing.PAGE_HEIGHT - PdfSpacing.FOOTER_HEIGHT
        canvas.drawLine(PdfSpacing.MARGIN, top, PdfSpacing.PAGE_WIDTH - PdfSpacing.MARGIN, top,
            builder.paints.stroke(palette.divider, 0.75f))
        val half = PdfSpacing.CONTENT_WIDTH / 2f
        val textTop = top + PdfSpacing.MD - 2f
        builder.text.text(canvas, builder.header.storeName, PdfSpacing.MARGIN + half, textTop, half,
            PdfTypeScale.caption.copy(bold = true), palette.textMuted, maxLines = 1)
        val info = "طُبع ${AppNumberFormat.dateTime(builder.header.printedAt)} · ${AppNumberFormat.pageOf(page, total)}"
        builder.text.text(canvas, info, PdfSpacing.MARGIN, textTop, half, PdfTypeScale.caption, palette.textMuted,
            Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1)
    }
}
