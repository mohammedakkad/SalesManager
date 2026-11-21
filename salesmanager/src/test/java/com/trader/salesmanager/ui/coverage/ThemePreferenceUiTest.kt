package com.trader.salesmanager.ui.coverage

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.trader.core.data.local.appDataStore
import com.trader.salesmanager.ui.theme.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.rules.ExternalResource
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Actual app theme, with an observable preference store and user-driven toggles. */
@RunWith(RobolectricTestRunner::class)
class ThemePreferenceUiTest:OfflineSellerUiTest() {
    private val preferences=MutableStateFlow<Preferences>(emptyPreferences())
    private var writes=0
    private val committedThemes=mutableListOf<String>()
    private val store=object:DataStore<Preferences> {
        override val data:Flow<Preferences> = preferences
        override suspend fun updateData(transform:suspend(Preferences)->Preferences):Preferences {
            writes++;return transform(preferences.value).also{preferences.value=it}
        }
    }
    @get:Rule(order=-1) val preferencesFixture=object:ExternalResource(){
        override fun before(){
            mockkStatic("com.trader.core.data.local.DataStoreProviderKt")
            every{any<Context>().appDataStore} returns store
        }
        override fun after(){unmockkStatic("com.trader.core.data.local.DataStoreProviderKt")}
    }
    @Composable private fun Probe() {
        val toggle=toggleTheme
        val snapshot="dark:$isDarkTheme/tokens:${appColors.isDark}/primary:${MaterialTheme.colorScheme.primary.toArgb()}"
        SideEffect{committedThemes+=snapshot}
        Column {
            Text(snapshot)
            Button(onClick=toggle){Text("تبديل المظهر")}
        }
    }
    private fun expected(dark:Boolean)="dark:$dark/tokens:$dark/primary:${(if(dark) Emerald400 else Emerald500).toArgb()}"
    @Test fun firstCompositionUsesPersistedDarkColorsWithoutAnOppositeInitialFrame(){
        preferences.value=preferencesOf(DARK_MODE_KEY to true)
        show{SalesManagerTheme(context=activity){Probe()}}
        ui.onNodeWithText(expected(true)).assertExists()
        ui.onNodeWithText(expected(false)).assertDoesNotExist();assertEquals(0,writes)
        assertTrue(committedThemes.isNotEmpty());assertTrue(committedThemes.all{it==expected(true)})
    }
    @Test fun missingPreferenceStartsLightAndTwoUserTogglesPersistTheCurrentChoice(){
        show{SalesManagerTheme{Probe()}}
        ui.onNodeWithText(expected(false)).assertExists()
        ui.onNodeWithText("تبديل المظهر").performClick()
        ui.onNodeWithText(expected(true)).assertExists()
        ui.runOnIdle{assertEquals(true,preferences.value[DARK_MODE_KEY])}
        ui.onNodeWithText("تبديل المظهر").performClick()
        ui.onNodeWithText(expected(false)).assertExists()
        ui.runOnIdle{assertEquals(false,preferences.value[DARK_MODE_KEY]);assertEquals(2,writes)}
    }
    @Test fun externalPreferenceChangesUpdateAnAlreadyComposedTheme(){
        show{SalesManagerTheme(context=activity){Probe()}}
        ui.onNodeWithText(expected(false)).assertExists()
        ui.runOnIdle{preferences.value=preferencesOf(DARK_MODE_KEY to true)}
        ui.onNodeWithText(expected(true)).assertExists()
        ui.runOnIdle{preferences.value=emptyPreferences()}
        ui.onNodeWithText(expected(false)).assertExists();assertEquals(0,writes)
    }
    @Test fun changingUnrelatedPreferencesPreservesTheSelectedDarkTheme(){
        preferences.value=preferencesOf(DARK_MODE_KEY to true)
        show{SalesManagerTheme{Probe()}}
        ui.onNodeWithText(expected(true)).assertExists()
        ui.runOnIdle{preferences.value=preferencesOf(DARK_MODE_KEY to true,stringPreferencesKey("store_name") to "متجر آخر")}
        ui.onNodeWithText(expected(true)).assertExists();assertEquals(0,writes)
    }
}
