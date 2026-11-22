package com.trader.salesmanager.util.pdf.components

import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** Section heading with an accent bar; keeps at least [keepWithNext] points of content on the same page. */
object PdfSectionTitle {
    fun draw(builder: PdfDocumentBuilder, title: String, keepWithNext: Float = 60f) {
        val height = builder.text.height(title, builder.width, PdfTypeScale.section, maxLines = 1)
        builder.ensureSpace(height + PdfSpacing.SM + keepWithNext)
        val top = builder.y
        builder.canvas.drawRoundRect(builder.right - 3f, top + 2f, builder.right, top + height - 2f, 1.5f, 1.5f,
            builder.paints.fill(builder.palette.brand))
        builder.text.text(builder.canvas, title, builder.left, top, builder.width - PdfSpacing.SM,
            PdfTypeScale.section, builder.palette.textPrimary, maxLines = 1)
        builder.log("section", top, top + height)
        builder.y = top + height + PdfSpacing.SM
    }
}
