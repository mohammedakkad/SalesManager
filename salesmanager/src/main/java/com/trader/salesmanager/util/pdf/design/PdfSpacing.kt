package com.trader.salesmanager.util.pdf.design

/** Spacing scale and page geometry (points). A4 = 595 x 842. */
object PdfSpacing {
    const val XS = 4f
    const val SM = 8f
    const val MD = 12f
    const val LG = 16f
    const val XL = 24f

    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 36f
    const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    const val HEADER_HEIGHT = 96f
    const val CONTINUATION_HEADER_HEIGHT = 56f
    const val FOOTER_HEIGHT = 36f
    const val CONTENT_BOTTOM = PAGE_HEIGHT - FOOTER_HEIGHT - MD

    const val CARD_RADIUS = 8f
    const val CARD_PADDING = MD
    const val TABLE_HEADER_HEIGHT = 26f
    const val TABLE_ROW_MIN_HEIGHT = 24f
    const val CELL_PADDING = 5f
}
