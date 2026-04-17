package com.trader.salesmanager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.trader.core.data.local.appDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════
//  DataStore key
// ══════════════════════════════════════════════════════════════════
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

// ══════════════════════════════════════════════════════════════════
//  CompositionLocal — يوفّر حالة الـ Theme لكل الشجرة
// ══════════════════════════════════════════════════════════════════
val LocalDarkTheme = compositionLocalOf { false }
val LocalToggleTheme = compositionLocalOf<() -> Unit> { {} }

// ══════════════════════════════════════════════════════════════════
//  Color Schemes
// ══════════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    primary              = Emerald500,
    onPrimary            = Color.White,
    primaryContainer     = Emerald100,
    onPrimaryContainer   = Emerald900,
    secondary            = Cyan500,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCCF2F8),
    onSecondaryContainer = Color(0xFF003E4D),
    tertiary             = Violet500,
    onTertiary           = Color.White,
    background           = LightBackground,
    onBackground         = LightOnSurface,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVar,
    outline              = LightBorder,
    outlineVariant       = LightDivider,
    error                = DebtRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    inverseSurface       = Slate800,
    inverseOnSurface     = Slate100,
    inversePrimary       = Emerald400,
    scrim                = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Emerald400,
    onPrimary            = Emerald900,
    primaryContainer     = Emerald700,
    onPrimaryContainer   = Emerald100,
    secondary            = Cyan400,
    onSecondary          = Color(0xFF003E4D),
    secondaryContainer   = Color(0xFF004F63),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary             = Violet400,
    onTertiary           = Color(0xFF290064),
    background           = DarkBackground,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVar,
    outline              = DarkBorder,
    outlineVariant       = DarkDivider,
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    inverseSurface       = DarkOnSurface,
    inverseOnSurface     = DarkSurface,
    inversePrimary       = Emerald700,
    scrim                = Color.Black,
)

// ══════════════════════════════════════════════════════════════════
//  Semantic Tokens — يُستخدم في الشاشات بدل الألوان الثابتة
// ══════════════════════════════════════════════════════════════════
data class AppColorTokens(
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBackgroundVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSubtle: Color,
    val divider: Color,
    val border: Color,
    val isDark: Boolean
)

private val LightTokens = AppColorTokens(
    screenBackground    = LightBackground,
    cardBackground      = LightSurface,
    cardBackgroundVariant = LightSurfaceVariant,
    textPrimary         = LightOnSurface,
    textSecondary       = LightOnSurfaceVar,
    textSubtle          = LightSubtle,
    divider             = LightDivider,
    border              = LightBorder,
    isDark              = false
)

private val DarkTokens = AppColorTokens(
    screenBackground    = DarkBackground,
    cardBackground      = DarkSurface,
    cardBackgroundVariant = DarkSurfaceVariant,
    textPrimary         = DarkOnSurface,
    textSecondary       = DarkOnSurfaceVar,
    textSubtle          = DarkSubtle,
    divider             = DarkDivider,
    border              = DarkBorder,
    isDark              = true
)

val LocalAppColors = compositionLocalOf { LightTokens }

// ══════════════════════════════════════════════════════════════════
//  Theme Composable
// ══════════════════════════════════════════════════════════════════
@Composable
fun SalesManagerTheme(
    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    // قراءة الإعداد من DataStore
    val isDark by context.appDataStore.data
        .map { it[DARK_MODE_KEY] ?: false }
        .collectAsState(initial = false)

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val tokens      = if (isDark) DarkTokens else LightTokens

    // دالة التبديل — تحفظ في DataStore فوراً
    val toggle: () -> Unit = {
        scope.launch {
            context.appDataStore.edit { it[DARK_MODE_KEY] = !isDark }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme    provides isDark,
        LocalToggleTheme  provides toggle,
        LocalAppColors    provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

// ══════════════════════════════════════════════════════════════════
//  Helpers — اختصارات للوصول من أي Composable
// ══════════════════════════════════════════════════════════════════
val appColors: AppColorTokens
    @Composable get() = LocalAppColors.current

val isDarkTheme: Boolean
    @Composable get() = LocalDarkTheme.current

val toggleTheme: () -> Unit
    @Composable get() = LocalToggleTheme.current
