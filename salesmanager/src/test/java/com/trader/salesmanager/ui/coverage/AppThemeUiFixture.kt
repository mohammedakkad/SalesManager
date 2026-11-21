package com.trader.salesmanager.ui.coverage

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.trader.core.data.local.appDataStore
import com.trader.salesmanager.ui.theme.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.rules.ExternalResource

/** Actual theme composition with deterministic external preference changes. No backend is opened. */
abstract class AppThemeUiFixture:OfflineSellerUiTest() {
    protected val preferences=MutableStateFlow<Preferences>(emptyPreferences())
    private val dataStore=object:DataStore<Preferences> {
        override val data:Flow<Preferences> = preferences
        override suspend fun updateData(transform:suspend(Preferences)->Preferences)=transform(preferences.value).also{preferences.value=it}
    }
    // Keep the fake installed until Compose and ActivityScenario have fully disposed.
    @get:Rule(order=-1) val appPreferences=object:ExternalResource() {
        override fun before(){
            mockkStatic("com.trader.core.data.local.DataStoreProviderKt")
            every{any<Context>().appDataStore} returns dataStore
        }
        override fun after(){unmockkStatic("com.trader.core.data.local.DataStoreProviderKt")}
    }
    @Volatile private var observedDark:Boolean?=null
    protected fun showApp(content:@Composable ()->Unit){
        ui.setContent{org.koin.compose.KoinContext{SalesManagerTheme{val value=isDarkTheme;SideEffect{observedDark=value};content()}}}
        if(!ui.mainClock.autoAdvance)ui.mainClock.advanceTimeBy(1000)
        ui.waitUntil(5000){observedDark==false}
    }
    protected fun dark(value:Boolean){
        ui.runOnIdle{preferences.value=preferencesOf(DARK_MODE_KEY to value)}
        if(!ui.mainClock.autoAdvance)ui.mainClock.advanceTimeBy(1000)
        ui.waitUntil(5000){observedDark==value}
    }
}
