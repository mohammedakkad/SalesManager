package com.trader.salesmanager.util.pdf.core

import androidx.core.graphics.withTranslation
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import com.trader.salesmanager.util.pdf.design.PdfTextStyle

/**
 * Text layout for RTL documents. Paragraphs are right-to-left (Arabic documents); numbers,
 * dates and identifiers arrive pre-isolated from AppNumberFormat, so they keep their own
 * left-to-right order instead of being forced into RTL.
 *
 * TextPaints are cached per style/color; callers measure a [StaticLayout] once with [layout]
 * and draw that same layout with [draw].
 */
class PdfTextPainter {
    private data class PaintKey(val size: Float, val bold: Boolean, val color: Int)
    private val paints = HashMap<PaintKey, TextPaint>()

    fun paint(style: PdfTextStyle, color: Int): TextPaint = paints.getOrPut(PaintKey(style.size, style.bold, color)) {
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = style.size
            typeface = PdfFonts.typeface(style.bold)
        }
    }

    fun layout(
        text: String,
        width: Float,
        style: PdfTextStyle,
        color: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = Int.MAX_VALUE
    ): StaticLayout {
        val safeWidth = width.toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint(style, color), safeWidth)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setLineSpacing(0f, style.lineSpacing)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    fun draw(canvas: Canvas, layout: StaticLayout, left: Float, top: Float) {
        canvas.withTranslation(left, top) { layout.draw(this) }
    }

    /** Lays out and draws in one call; returns the drawn height. */
    fun text(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        style: PdfTextStyle,
        color: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = Int.MAX_VALUE
    ): Float {
        val layout = layout(text, width, style, color, alignment, maxLines)
        draw(canvas, layout, left, top)
        return layout.height.toFloat()
    }

    fun height(text: String, width: Float, style: PdfTextStyle, maxLines: Int = Int.MAX_VALUE): Float =
        layout(text, width, style, 0, maxLines = maxLines).height.toFloat()

    fun lineHeight(style: PdfTextStyle): Float = height("ا", 200f, style)
}
