package com.trader.salesmanager.util.pdf.design

/**
 * Per-document rendering options.
 * @param printFriendly low-ink, grayscale-safe palette.
 * @param watermark optional diagonal watermark text (e.g. "نسخة"); null for none.
 */
data class PdfOptions(
    val printFriendly: Boolean = false,
    val watermark: String? = null
) {
    val palette: PdfPalette get() = if (printFriendly) PdfPalette.PRINT else PdfPalette.STANDARD

    companion object {
        val DEFAULT = PdfOptions()
    }
}
