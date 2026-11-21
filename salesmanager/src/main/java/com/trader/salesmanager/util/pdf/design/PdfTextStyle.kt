package com.trader.salesmanager.util.pdf.design

/** A text role: size in PDF points (1/72 inch) and weight. Color is supplied per use from the palette. */
data class PdfTextStyle(val size: Float, val bold: Boolean = false, val lineSpacing: Float = 1.12f)
