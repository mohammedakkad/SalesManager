package com.trader.salesmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary       = PrimaryGreen,
    onPrimary     = OnPrimary,
    secondary     = SecondaryTeal,
    background    = BackgroundLight,
    surface       = SurfaceLight,
    error         = ErrorRed,
    onBackground  = TextPrimary,
    onSurface     = TextPrimary
)

private val DarkColorScheme = darkColorScheme(
    primary    = Color(0xFF81C784),
    secondary  = Color(0xFF4DB6AC),
    background = Color(0xFF121212),
    surface    = Color(0xFF1E1E1E)
)

@Composable
fun SalesManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}