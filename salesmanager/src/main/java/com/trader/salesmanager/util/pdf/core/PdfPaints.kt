package com.trader.salesmanager.util.pdf.core

import android.graphics.Paint

/** Caches fill/stroke paints per color (and width) for the lifetime of one document. */
class PdfPaints {
    private val fills = HashMap<Int, Paint>()
    private val strokes = HashMap<Long, Paint>()

    fun fill(color: Int): Paint = fills.getOrPut(color) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    }

    fun stroke(color: Int, width: Float = 0.75f): Paint {
        val key = (color.toLong() shl 32) or (width.toRawBits().toLong() and 0xffffffffL)
        return strokes.getOrPut(key) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width }
        }
    }
}
