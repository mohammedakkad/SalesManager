package com.trader.salesmanager.util.pdf.core

/** One placed block, recorded so tests can verify pagination without parsing pixels. */
data class PdfLayoutEntry(val page: Int, val kind: String, val top: Float, val bottom: Float)
