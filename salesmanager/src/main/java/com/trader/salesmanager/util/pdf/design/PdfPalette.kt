package com.trader.salesmanager.util.pdf.design

import android.graphics.Color

/**
 * Color roles for PDF documents. Components only use roles, never raw colors, so the
 * whole document can switch to the low-ink [PRINT] variant.
 */
data class PdfPalette(
    val brand: Int,
    val onBrand: Int,
    val onBrandMuted: Int,
    /** Filled header band; transparent in the print variant (no ink-heavy blocks). */
    val headerBand: Int,
    val headerText: Int,
    val headerTextMuted: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val border: Int,
    val divider: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textMuted: Int,
    val tableHeader: Int,
    val onTableHeader: Int,
    val success: Int,
    val successContainer: Int,
    val warning: Int,
    val warningContainer: Int,
    val danger: Int,
    val dangerContainer: Int,
    val info: Int,
    val infoContainer: Int,
    val accent: Int,
    val watermark: Int
) {
    companion object {
        private fun rgb(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

        /** Emerald brand palette (the existing app identity). */
        val STANDARD = PdfPalette(
            brand = rgb(4, 120, 87),
            onBrand = Color.WHITE,
            onBrandMuted = rgb(209, 250, 229),
            headerBand = rgb(4, 120, 87),
            headerText = Color.WHITE,
            headerTextMuted = rgb(209, 250, 229),
            surface = Color.WHITE,
            surfaceAlt = rgb(248, 250, 252),
            border = rgb(226, 232, 240),
            divider = rgb(226, 232, 240),
            textPrimary = rgb(15, 23, 42),
            textSecondary = rgb(51, 65, 85),
            textMuted = rgb(100, 116, 139),
            tableHeader = rgb(6, 78, 59),
            onTableHeader = Color.WHITE,
            success = rgb(4, 120, 87),
            successContainer = rgb(209, 250, 229),
            warning = rgb(180, 83, 9),
            warningContainer = rgb(254, 243, 199),
            danger = rgb(185, 28, 28),
            dangerContainer = rgb(254, 226, 226),
            info = rgb(14, 116, 144),
            infoContainer = rgb(207, 250, 254),
            accent = rgb(109, 40, 217),
            watermark = Color.argb(22, 4, 120, 87)
        )

        /**
         * Low-ink, grayscale-safe variant: white backgrounds, outlines instead of fills,
         * and meaning carried by text/weight rather than color alone.
         */
        val PRINT = PdfPalette(
            brand = rgb(17, 17, 17),
            onBrand = Color.WHITE,
            onBrandMuted = rgb(80, 80, 80),
            headerBand = Color.TRANSPARENT,
            headerText = rgb(17, 17, 17),
            headerTextMuted = rgb(80, 80, 80),
            surface = Color.WHITE,
            surfaceAlt = rgb(245, 245, 245),
            border = rgb(160, 160, 160),
            divider = rgb(200, 200, 200),
            textPrimary = rgb(17, 17, 17),
            textSecondary = rgb(51, 51, 51),
            textMuted = rgb(90, 90, 90),
            tableHeader = rgb(235, 235, 235),
            onTableHeader = rgb(17, 17, 17),
            success = rgb(17, 17, 17),
            successContainer = Color.WHITE,
            warning = rgb(17, 17, 17),
            warningContainer = Color.WHITE,
            danger = rgb(17, 17, 17),
            dangerContainer = Color.WHITE,
            info = rgb(17, 17, 17),
            infoContainer = Color.WHITE,
            accent = rgb(60, 60, 60),
            watermark = Color.argb(18, 0, 0, 0)
        )
    }
}
