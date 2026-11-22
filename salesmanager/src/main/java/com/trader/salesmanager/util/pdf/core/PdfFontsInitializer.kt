package com.trader.salesmanager.util.pdf.core

import android.content.Context
import androidx.startup.Initializer

/** Loads the bundled PDF font at process start (app and instrumentation alike). */
class PdfFontsInitializer : Initializer<Unit> {
    override fun create(context: Context) = PdfFonts.install(context)
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
