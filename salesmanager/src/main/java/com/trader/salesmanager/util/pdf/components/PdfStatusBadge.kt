package com.trader.salesmanager.util.pdf.components

import android.graphics.Canvas
import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Pill-shaped status label (paid / on credit / returned). Always outlined so it survives grayscale printing. */
object PdfStatusBadge {
    const val HEIGHT = 16f

    fun label(tone: PdfBadgeTone): String = when (tone) {
        PdfBadgeTone.PAID -> "مدفوع"
        PdfBadgeTone.CREDIT -> "آجل"
        PdfBadgeTone.RETURNED -> "مُرتجع"
        PdfBadgeTone.PARTIAL -> "مرتجع جزئي"
        PdfBadgeTone.NEUTRAL -> ""
    }

    /** Draws the badge centered on [centerX]; returns its width. */
    fun draw(canvas: Canvas, builder: PdfDocumentBuilder, text: String, tone: PdfBadgeTone, centerX: Float, top: Float): Float {
        val palette = builder.palette
        val (fg, bg) = when (tone) {
            PdfBadgeTone.PAID -> palette.success to palette.successContainer
            PdfBadgeTone.CREDIT -> palette.danger to palette.dangerContainer
            PdfBadgeTone.RETURNED, PdfBadgeTone.PARTIAL -> palette.warning to palette.warningContainer
            PdfBadgeTone.NEUTRAL -> palette.textSecondary to palette.surfaceAlt
        }
        val paint = builder.text.paint(PdfTypeScale.badge, fg)
        val width = paint.measureText(text) + PdfSpacing.MD * 2
        val left = centerX - width / 2f
        canvas.drawRoundRect(left, top, left + width, top + HEIGHT, HEIGHT / 2f, HEIGHT / 2f, builder.paints.fill(bg))
        canvas.drawRoundRect(left, top, left + width, top + HEIGHT, HEIGHT / 2f, HEIGHT / 2f, builder.paints.stroke(fg, 0.75f))
        val layout = builder.text.layout(text, width, PdfTypeScale.badge, fg, Layout.Alignment.ALIGN_CENTER, 1)
        builder.text.draw(canvas, layout, left, top + (HEIGHT - layout.height) / 2f)
        return width
    }
}
