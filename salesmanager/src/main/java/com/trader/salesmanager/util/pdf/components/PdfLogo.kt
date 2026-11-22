package com.trader.salesmanager.util.pdf.components

import androidx.core.graphics.withClip
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfTypeScale
import kotlin.math.min

/** Store logo in a circle (center-cropped), or the store's first letter when no logo is set. */
object PdfLogo {
    const val SIZE = 56f

    fun draw(canvas: Canvas, builder: PdfDocumentBuilder, left: Float, top: Float, logo: Bitmap?, storeName: String) {
        val palette = builder.palette
        val radius = SIZE / 2f
        val cx = left + radius
        val cy = top + radius
        canvas.drawCircle(cx, cy, radius, builder.paints.fill(palette.surface))
        if (logo != null && logo.width > 0 && logo.height > 0) {
            val side = min(logo.width, logo.height)
            val src = Rect((logo.width - side) / 2, (logo.height - side) / 2, (logo.width + side) / 2, (logo.height + side) / 2)
            canvas.withClip(Path().apply { addCircle(cx, cy, radius - 2f, Path.Direction.CW) }) {
                drawBitmap(logo, src, RectF(cx - radius, cy - radius, cx + radius, cy + radius), null)
            }
        } else {
            val initial = storeName.trim().firstOrNull()?.toString() ?: "م"
            val layout = builder.text.layout(initial, SIZE, PdfTypeScale.display, palette.brand, Layout.Alignment.ALIGN_CENTER, 1)
            builder.text.draw(canvas, layout, left, cy - layout.height / 2f)
        }
        canvas.drawCircle(cx, cy, radius - 0.5f, builder.paints.stroke(palette.border, 1f))
    }
}
