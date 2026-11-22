package com.trader.salesmanager.ui.locale

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * THE SINGLE SWITCH for the app's language and layout direction.
 *
 * The app is Arabic-only today, so it forces Arabic resources and right-to-left layout no matter
 * which language the device uses (an English device must look exactly like an Arabic one).
 * When English is added in the future, relax this object (e.g. follow the per-app language from
 * res/xml/locales_config.xml) instead of changing screens: everything else reads its locale and
 * direction from here or from resources.
 *
 * Applied in MainActivity.attachBaseContext (resources/configuration) and SalesManagerTheme
 * (Compose layout direction).
 */
object AppLocale {
    val locale: Locale = Locale.forLanguageTag("ar")
    val layoutDirection: LayoutDirection = LayoutDirection.Rtl

    /** Returns [base] with the app locale and its layout direction applied to the configuration. */
    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
