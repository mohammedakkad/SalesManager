package com.trader.salesmanager.util.pdf.core

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.trader.salesmanager.R

/**
 * Bundled IBM Plex Sans Arabic (SIL OFL 1.1, license in assets/licenses), loaded once.
 * Using the bundled font makes PDF output identical on every device. If [install] has not
 * run (e.g. a plain JVM test), the system sans-serif is used so generation never fails.
 */
object PdfFonts {
    @Volatile private var regularFont: Typeface? = null
    @Volatile private var boldFont: Typeface? = null

    val isInstalled: Boolean get() = regularFont != null && boldFont != null

    @Synchronized
    fun install(context: Context) {
        if (isInstalled) return
        val app = context.applicationContext ?: context
        regularFont = runCatching { ResourcesCompat.getFont(app, R.font.ibm_plex_sans_arabic_regular) }.getOrNull()
        boldFont = runCatching { ResourcesCompat.getFont(app, R.font.ibm_plex_sans_arabic_bold) }.getOrNull()
    }

    fun typeface(bold: Boolean): Typeface =
        (if (bold) boldFont else regularFont)
            ?: Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
}
