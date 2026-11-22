package com.trader.salesmanager.util.pdf.core

import android.graphics.Canvas

/** Destination for finished pages: a real PdfDocument in the app, bitmaps in previews/tests. */
interface PdfPageSink {
    fun beginPage(pageNumber: Int, width: Int, height: Int): Canvas
    fun endPage()
}
