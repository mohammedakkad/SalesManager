package com.trader.salesmanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.trader.salesmanager.R

/** Bundled IBM Plex Sans Arabic (SIL OFL 1.1) — the same family as the PDF documents. */
val AppFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold,   fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge    = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium   = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall    = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
