package com.trader.salesmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
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
    onSurface          = Slate800,
    surfaceVariant     = Slate100,
    onSurfaceVariant   = Slate600,
    outline            = Slate200,
    error              = DebtRed,
    onError            = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary            = Emerald400,
    onPrimary          = Emerald900,
    primaryContainer   = Emerald700,
    onPrimaryContainer = Emerald100,
    secondary          = Cyan400,
    onSecondary        = Dark900,
    tertiary           = Color(0xFFA78BFA),
    background         = Dark950,
    onBackground       = Slate100,
    surface            = Dark900,
    onSurface          = Slate100,
    surfaceVariant     = Dark800,
    onSurfaceVariant   = Slate400,
    outline            = Dark700,
    error              = Color(0xFFF87171),
    onError            = Color(0xFF7F1D1D),
)

@Composable
fun SalesManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography,
        content     = content
    )
}
