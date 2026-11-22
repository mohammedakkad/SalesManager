package com.trader.core.domain.repository

import kotlinx.coroutines.flow.Flow

data class LowStockAlertSettings(
    val enabled: Boolean = true,
    val preferredHour: Int = 9
) {
    companion object {
        const val MORNING_HOUR = 9
        const val EVENING_HOUR = 18
    }
}

interface LowStockSettingsRepository {
    val settings: Flow<LowStockAlertSettings>
    suspend fun getSettings(): LowStockAlertSettings
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setPreferredHour(hour: Int)
}
