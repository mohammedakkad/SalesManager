package com.trader.salesmanager.util.pdf.core

import android.graphics.Bitmap

/**
 * What the shared header/footer show on every page.
 * @param documentNumber already formatted (isolated) identifier, or null.
 * @param details short key/value lines printed in the first-page header (e.g. the month).
 */
data class PdfHeaderContent(
    val storeName: String,
    val documentTitle: String,
    val documentNumber: String? = null,
    val details: List<String> = emptyList(),
    val logo: Bitmap? = null,
    val printedAt: Long = System.currentTimeMillis()
)
