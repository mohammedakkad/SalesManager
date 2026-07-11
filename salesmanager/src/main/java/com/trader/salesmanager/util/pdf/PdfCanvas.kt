package com.trader.salesmanager.util.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.max

internal object PdfCanvas {
    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    fun stroke(color: Int, width: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    fun roundRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = PdfLayoutConstants.CARD_RADIUS,
        borderColor: Int? = null
    ) {
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, radius, radius, fill(color))
        borderColor?.let {
            canvas.drawRoundRect(rect, radius, radius, stroke(it))
        }
    }

    fun text(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = Int.MAX_VALUE
    ): Float {
        val layout = layout(text, width, size, color, bold, alignment, maxLines)
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    fun cellText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
        maxLines: Int = 2
    ) {
        val inset = 4f
        val layout = layout(text, width - inset * 2f, size, color, bold, alignment, maxLines)
        val textTop = top + max(0f, (height - layout.height) / 2f)
        canvas.save()
        canvas.clipRect(left, top, left + width, top + height)
        canvas.translate(left + inset, textTop)
        layout.draw(canvas)
        canvas.restore()
    }

    fun measureHeight(
        text: String,
        width: Float,
        size: Float,
        bold: Boolean = false,
        maxLines: Int = Int.MAX_VALUE
    ): Float = layout(
        text = text,
        width = width,
        size = size,
        color = PdfLayoutConstants.SLATE_900,
        bold = bold,
        alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines = maxLines
    ).height.toFloat()

    private fun layout(
        text: String,
        width: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        alignment: Layout.Alignment,
        maxLines: Int
    ): StaticLayout {
        val safeWidth = width.toInt().coerceAtLeast(1)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(
                "sans-serif",
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.08f)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }
}
