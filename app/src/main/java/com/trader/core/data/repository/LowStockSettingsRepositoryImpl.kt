package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.repository.LowStockAlertSettings
import com.trader.core.domain.repository.LowStockSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LowStockSettingsRepositoryImpl(
    private val context: Context
) : LowStockSettingsRepository {

    override val settings: Flow<LowStockAlertSettings> =
        context.appDataStore.data.map { preferences ->
            LowStockAlertSettings(
                enabled = preferences[ENABLED_KEY] ?: true,
                preferredHour = preferences[PREFERRED_HOUR_KEY]
                    ?: LowStockAlertSettings.MORNING_HOUR
            )
        }

    override suspend fun getSettings(): LowStockAlertSettings = settings.first()

    override suspend fun setEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[ENABLED_KEY] = enabled }
    }

    override suspend fun setPreferredHour(hour: Int) {
        require(hour in 0..23)
        context.appDataStore.edit { it[PREFERRED_HOUR_KEY] = hour }
    }

    companion object {
        val ENABLED_KEY = booleanPreferencesKey("low_stock_alerts_enabled")
        val PREFERRED_HOUR_KEY = intPreferencesKey("low_stock_alert_preferred_hour")
    }
}
