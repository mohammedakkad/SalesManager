package com.trader.admin.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Admin always uses dark theme
private val AdminDarkColors = darkColorScheme(
    primary            = Indigo500,
    onPrimary          = Color.White,
    primaryContainer   = Navy800,
    onPrimaryContainer = Indigo300,
    secondary          = Violet500,
    onSecondary        = Color.White,
    tertiary           = Cyan500,
    background         = Navy950,
    onBackground       = Slate100,
    surface            = Navy900,
    onSurface          = Slate100,
    surfaceVariant     = Slate800,
    onSurfaceVariant   = Slate400,
    outline            = Slate700,
    error              = Rose500,
    onError            = Color.White,
)

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdminDarkColors,
        typography  = AdminTypography,
        content     = content
    )
}
