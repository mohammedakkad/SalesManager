package com.trader.salesmanager.util.pdf.design

/** Type scale for A4 documents (points). */
object PdfTypeScale {
    val display = PdfTextStyle(20f, bold = true)
    val title = PdfTextStyle(14f, bold = true)
    val section = PdfTextStyle(12f, bold = true)
    val body = PdfTextStyle(10f)
    val bodyStrong = PdfTextStyle(10f, bold = true)
    val label = PdfTextStyle(9f, bold = true)
    val tableHeader = PdfTextStyle(9f, bold = true)
    val tableCell = PdfTextStyle(9f)
    val tableCellStrong = PdfTextStyle(9f, bold = true)
    val metric = PdfTextStyle(15f, bold = true)
    val total = PdfTextStyle(12f, bold = true)
    val caption = PdfTextStyle(8f)
    val badge = PdfTextStyle(8f, bold = true)
}
