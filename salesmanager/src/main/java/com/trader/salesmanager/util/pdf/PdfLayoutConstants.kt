package com.trader.salesmanager.util.pdf

import android.graphics.Color

object PdfLayoutConstants {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 36f
    const val HEADER_HEIGHT = 102f
    const val CONTINUATION_HEADER_HEIGHT = 72f
    const val FOOTER_HEIGHT = 42f
    const val CONTENT_BOTTOM = PAGE_HEIGHT - FOOTER_HEIGHT - 12f
    const val CARD_RADIUS = 10f
    const val TABLE_HEADER_HEIGHT = 30f
    const val TABLE_ROW_MIN_HEIGHT = 32f

    val EMERALD_900: Int = Color.rgb(6, 78, 59)
    val EMERALD_700: Int = Color.rgb(4, 120, 87)
    val EMERALD_500: Int = Color.rgb(16, 185, 129)
    val EMERALD_100: Int = Color.rgb(209, 250, 229)
    val CYAN_500: Int = Color.rgb(6, 182, 212)
    val CYAN_100: Int = Color.rgb(207, 250, 254)
    val VIOLET_500: Int = Color.rgb(139, 92, 246)
    val AMBER_500: Int = Color.rgb(245, 158, 11)
    val AMBER_100: Int = Color.rgb(254, 243, 199)
    val RED_500: Int = Color.rgb(239, 68, 68)
    val RED_100: Int = Color.rgb(254, 226, 226)
    val SLATE_900: Int = Color.rgb(15, 23, 42)
    val SLATE_700: Int = Color.rgb(51, 65, 85)
    val SLATE_600: Int = Color.rgb(71, 85, 105)
    val SLATE_400: Int = Color.rgb(148, 163, 184)
    val SLATE_200: Int = Color.rgb(226, 232, 240)
    val SLATE_100: Int = Color.rgb(241, 245, 249)
    val SLATE_50: Int = Color.rgb(248, 250, 252)
    val WHITE: Int = Color.WHITE
    val TRANSPARENT: Int = Color.TRANSPARENT
}
