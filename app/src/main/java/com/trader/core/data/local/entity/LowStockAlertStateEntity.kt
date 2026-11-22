package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "low_stock_alert_states"
)
data class LowStockAlertStateEntity(
    @PrimaryKey val unitId: String,
    val lastStatus: String,
    val lastNotifiedStatus: String?,
    val lastNotifiedAt: Long?
)
