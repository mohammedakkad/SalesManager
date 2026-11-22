package com.trader.salesmanager.util.pdf.components

import android.text.Layout

/** Table column in reading order (the first column is the right-most one). */
data class PdfColumn(
    val title: String,
    val weight: Float,
    val alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
)
