package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "low_stock_alert_states",
    foreignKeys = [
        ForeignKey(
            entity = ProductUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LowStockAlertStateEntity(
    @androidx.room.PrimaryKey val unitId: String,
    val lastStatus: String,
    val lastNotifiedAt: Long?
)
