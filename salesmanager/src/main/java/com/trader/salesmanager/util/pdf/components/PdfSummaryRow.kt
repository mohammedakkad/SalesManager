package com.trader.salesmanager.util.pdf.components

/** A label/value line in a summary card. [emphasized] rows are printed larger after a divider. */
data class PdfSummaryRow(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
    val tone: PdfBadgeTone? = null
)
