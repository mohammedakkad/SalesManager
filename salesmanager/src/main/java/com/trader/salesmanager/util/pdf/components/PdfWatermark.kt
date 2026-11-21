package com.trader.salesmanager.util.pdf.components

import androidx.core.graphics.withRotation
import android.graphics.Canvas
import android.text.Layout
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTextStyle

/** Optional faint diagonal watermark (e.g. "نسخة" / "مسودة"). */
object PdfWatermark {
    fun draw(canvas: Canvas, text: String, builder: PdfDocumentBuilder) {
        val width = PdfSpacing.PAGE_WIDTH * 0.9f
        val layout = builder.text.layout(text, width, PdfTextStyle(72f, bold = true), builder.palette.watermark,
            Layout.Alignment.ALIGN_CENTER, 1)
        canvas.withRotation(-30f, PdfSpacing.PAGE_WIDTH / 2f, PdfSpacing.PAGE_HEIGHT / 2f) {
            builder.text.draw(this, layout, (PdfSpacing.PAGE_WIDTH - width) / 2f, PdfSpacing.PAGE_HEIGHT / 2f - layout.height / 2f)
        }
    }
}
