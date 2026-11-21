package com.trader.salesmanager.util.pdf.core

import android.graphics.Canvas
import android.graphics.Picture
import com.trader.salesmanager.util.pdf.components.PdfFooter
import com.trader.salesmanager.util.pdf.components.PdfHeader
import com.trader.salesmanager.util.pdf.components.PdfWatermark
import com.trader.salesmanager.util.pdf.design.PdfOptions
import com.trader.salesmanager.util.pdf.design.PdfPalette
import com.trader.salesmanager.util.pdf.design.PdfSpacing

/**
 * Pagination engine shared by every document.
 *
 * Page content is recorded into one [Picture] per page; when the document is complete the
 * pages are replayed into a [PdfPageSink] together with the consistent header/footer, which
 * can therefore print "page X of Y". Text stays vector text when replayed into a PdfDocument.
 */
class PdfDocumentBuilder(
    val header: PdfHeaderContent,
    val options: PdfOptions = PdfOptions.DEFAULT
) {
    val palette: PdfPalette = options.palette
    val text = PdfTextPainter()
    val paints = PdfPaints()

    private val pages = mutableListOf<Picture>()
    private var recording: Picture? = null
    private var currentCanvas: Canvas? = null

    /** Placed blocks per page (1-based page numbers), for verification. */
    val layoutLog = mutableListOf<PdfLayoutEntry>()

    /** Current vertical position on the current page. */
    var y: Float = 0f

    val pageNumber: Int get() = pages.size + if (recording != null) 1 else 0

    val canvas: Canvas
        get() = currentCanvas ?: newPage().let { currentCanvas!! }

    val left: Float get() = PdfSpacing.MARGIN
    val right: Float get() = PdfSpacing.PAGE_WIDTH - PdfSpacing.MARGIN
    val width: Float get() = PdfSpacing.CONTENT_WIDTH
    val bottom: Float get() = PdfSpacing.CONTENT_BOTTOM

    fun remaining(): Float = bottom - y

    /** Finishes the current page (if any) and starts a new one. */
    fun newPage() {
        finishRecording()
        val picture = Picture()
        recording = picture
        currentCanvas = picture.beginRecording(PdfSpacing.PAGE_WIDTH, PdfSpacing.PAGE_HEIGHT)
        y = PdfHeader.contentTop(continuation = pageNumber > 1)
    }

    /**
     * Ensures [height] fits on the current page, otherwise starts a new page and runs
     * [onNewPage] (e.g. to repeat a table header). Returns true when a page break happened.
     */
    fun ensureSpace(height: Float, onNewPage: (() -> Unit)? = null): Boolean {
        if (recording == null) newPage()
        if (y + height <= bottom) return false
        newPage()
        onNewPage?.invoke()
        return true
    }

    fun gap(height: Float) {
        y = (y + height).coerceAtMost(bottom)
    }

    fun log(kind: String, top: Float, bottom: Float) {
        layoutLog += PdfLayoutEntry(pageNumber, kind, top, bottom)
    }

    /** Replays every page into [sink] with header, footer and optional watermark. */
    fun renderTo(sink: PdfPageSink) {
        if (recording == null && pages.isEmpty()) newPage()
        finishRecording()
        val total = pages.size
        val headerPainter = PdfHeader(this)
        val footerPainter = PdfFooter(this)
        pages.forEachIndexed { index, picture ->
            val page = index + 1
            val target = sink.beginPage(page, PdfSpacing.PAGE_WIDTH, PdfSpacing.PAGE_HEIGHT)
            target.drawColor(palette.surface)
            headerPainter.draw(target, continuation = page > 1)
            target.drawPicture(picture)
            footerPainter.draw(target, page, total)
            options.watermark?.let { PdfWatermark.draw(target, it, this) }
            sink.endPage()
        }
    }

    val pageCount: Int get() = pages.size + if (recording != null) 1 else 0

    private fun finishRecording() {
        recording?.let {
            it.endRecording()
            pages += it
        }
        recording = null
        currentCanvas = null
    }
}
