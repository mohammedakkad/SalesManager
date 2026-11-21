package com.trader.salesmanager.util.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import com.trader.salesmanager.util.pdf.core.PdfPageSink
import java.io.File
import java.io.FileOutputStream

/** Test/preview sink: renders each page into a bitmap (native Robolectric graphics) and saves PNGs. */
class BitmapPageSink(private val directory: File, private val name: String, private val scale: Float = 2f) : PdfPageSink {
    val files = mutableListOf<File>()
    val pages = mutableListOf<Bitmap>()
    private var bitmap: Bitmap? = null

    override fun beginPage(pageNumber: Int, width: Int, height: Int): Canvas {
        val page = Bitmap.createBitmap((width * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
        bitmap = page
        return Canvas(page).apply { scale(scale, scale) }
    }

    override fun endPage() {
        val page = bitmap ?: return
        directory.mkdirs()
        val file = File(directory, "$name-p${pages.size + 1}.png")
        FileOutputStream(file).use { page.compress(Bitmap.CompressFormat.PNG, 100, it) }
        files += file
        pages += page
        bitmap = null
    }
}
