package com.trader.salesmanager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// نستخدم Light فقط لتجنب مشاكل النص غير المرئي على أجهزة Dark Mode
private val AppColors = lightColorScheme(
    primary            = Emerald500,
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary          = Cyan500,
    onSecondary        = Color(0xFFFFFFFF),
    tertiary           = Violet500,
    background         = Slate50,
    onBackground       = Slate900,
    surface            = Color(0xFFFFFFFF),
    onSurface          = Slate800,        // ← نص داكن دائماً على Surface
    surfaceVariant     = Slate100,
    onSurfaceVariant   = Slate600,
    outline            = Slate200,
    error              = DebtRed,
    onError            = Color(0xFFFFFFFF),
)

@Composable
fun SalesManagerTheme(content: @Composable () -> Unit) {
    // نثبّت Light دائماً بغض النظر عن إعداد الجهاز
    // هذا يحل مشكلة النص غير المرئي في TextField على بعض الأجهزة
    MaterialTheme(
        colorScheme = AppColors,
        typography  = Typography,
        content     = content
    )
}
