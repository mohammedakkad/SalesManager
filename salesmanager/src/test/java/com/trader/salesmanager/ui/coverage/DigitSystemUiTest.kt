package com.trader.salesmanager.ui.coverage

import android.content.res.Configuration
import android.view.View
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.preferences.core.preferencesOf
import com.trader.core.util.format.AppNumberFormat
import com.trader.core.util.format.DigitSystemPreference
import com.trader.salesmanager.ui.locale.AppLocale
import com.trader.salesmanager.ui.settings.DigitSystemSettingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Device-language independence: the same screens under an English and an Arabic device
 * configuration must be right-to-left and use the user's digit system (default Arabic-Indic).
 */
@RunWith(RobolectricTestRunner::class)
class DigitSystemUiTest : AppThemeUiFixture() {
    private val directions = mutableListOf<LayoutDirection>()

    // The base class pins Western digits for legacy fixtures; this test exercises the real default.
    @Before fun realDefault() { AppNumberFormat.digits = AppNumberFormat.DEFAULT_DIGITS }

    private fun probe() = showApp {
        val direction = LocalLayoutDirection.current
        SideEffect { directions += direction }
        Text(AppNumberFormat.number(1234.5, "%,.2f"))
    }

    @Config(qualifiers = "en") @Test fun englishDeviceIsRightToLeftWithArabicIndicDigits() {
        probe()
        ui.onNodeWithText("١٬٢٣٤٫٥٠").assertExists()
        assertTrue(directions.isNotEmpty() && directions.all { it == LayoutDirection.Rtl })
    }

    @Config(qualifiers = "ar") @Test fun arabicDeviceRendersTheSameAsAnEnglishDevice() {
        probe()
        ui.onNodeWithText("١٬٢٣٤٫٥٠").assertExists()
        assertTrue(directions.all { it == LayoutDirection.Rtl })
    }

    @Config(qualifiers = "en") @Test fun storedWesternChoiceIsAppliedBeforeTheFirstFrame() {
        preferences.value = preferencesOf(DigitSystemPreference.KEY to AppNumberFormat.DigitSystem.LATIN.name)
        val committed = mutableListOf<String>()
        showApp {
            val text = AppNumberFormat.number(1234.5, "%,.2f")
            SideEffect { committed += text }
            Text(text)
        }
        ui.onNodeWithText("1,234.50").assertExists()
        assertTrue("No Arabic-Indic frame before the stored choice: $committed", committed.all { it == "1,234.50" })
    }

    @Config(qualifiers = "en") @Test fun switchingTheSettingRecomposesNumbersAndPersistsTheChoice() {
        showApp {
            DigitSystemSettingItem()
            Text(AppNumberFormat.number(1234.5, "%,.2f"))
        }
        ui.onNodeWithText("١٬٢٣٤٫٥٠").assertExists()
        ui.onNodeWithText("غربية 123").performClick()
        ui.onNodeWithText("1,234.50").assertExists()
        ui.runOnIdle { assertEquals("LATIN", preferences.value[DigitSystemPreference.KEY]) }
        ui.onNodeWithText("عربية ١٢٣").performClick()
        ui.onNodeWithText("١٬٢٣٤٫٥٠").assertExists()
    }

    @Config(qualifiers = "en") @Test fun activityConfigurationIsArabicAndRightToLeftOnAnEnglishDevice() {
        val wrapped = AppLocale.wrap(activity)
        assertEquals("ar", wrapped.resources.configuration.locales[0].language)
        assertEquals(View.LAYOUT_DIRECTION_RTL, wrapped.resources.configuration.layoutDirection)
        assertEquals(Configuration.SCREENLAYOUT_LAYOUTDIR_RTL,
            wrapped.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
    }
}
