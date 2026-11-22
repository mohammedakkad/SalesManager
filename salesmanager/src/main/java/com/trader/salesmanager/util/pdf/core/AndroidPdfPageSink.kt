package com.trader.salesmanager.util.pdf.core

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/** Writes pages into an android.graphics.pdf.PdfDocument. */
class AndroidPdfPageSink : PdfPageSink {
    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null

    override fun beginPage(pageNumber: Int, width: Int, height: Int): Canvas {
        check(page == null) { "Previous page was not finished" }
        val started = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        page = started
        return started.canvas
    }

    override fun endPage() {
        page?.let(document::finishPage)
        page = null
    }

    fun writeTo(file: File): File {
        FileOutputStream(file).use(document::writeTo)
        return file
    }

    fun close() {
        if (page != null) endPage()
        document.close()
    }
}
