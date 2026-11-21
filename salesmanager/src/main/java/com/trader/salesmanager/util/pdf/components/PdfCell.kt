package com.trader.salesmanager.util.pdf.components

import com.trader.salesmanager.util.pdf.design.PdfTextStyle
import com.trader.salesmanager.util.pdf.design.PdfTypeScale

/** A table cell: text, optional color override, or a status badge instead of text. */
data class PdfCell(
    val text: String,
    val style: PdfTextStyle = PdfTypeScale.tableCell,
    val color: Int? = null,
    val badge: PdfBadgeTone? = null,
    val maxLines: Int = 3
)
