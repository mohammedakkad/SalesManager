package com.trader.core.util.format

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trader.core.data.local.appDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists the user's digit system ("نظام الأرقام") in the app DataStore and applies it to [AppNumberFormat]. */
object DigitSystemPreference {
    val KEY = stringPreferencesKey("digit_system")

    /** The stored choice, or null when the user never changed it (the default then applies). */
    fun read(preferences: Preferences): AppNumberFormat.DigitSystem? =
        preferences[KEY]?.let { stored -> AppNumberFormat.DigitSystem.entries.firstOrNull { it.name == stored } }

    /** Applies a stored choice to the shared formatter; an absent choice leaves the current value. */
    fun apply(preferences: Preferences) {
        read(preferences)?.let { AppNumberFormat.digits = it }
    }

    fun observe(context: Context): Flow<AppNumberFormat.DigitSystem> =
        context.appDataStore.data.map { read(it) ?: AppNumberFormat.DEFAULT_DIGITS }.distinctUntilChanged()

    suspend fun set(context: Context, system: AppNumberFormat.DigitSystem) {
        context.appDataStore.edit { it[KEY] = system.name }
        AppNumberFormat.digits = system
    }
}
