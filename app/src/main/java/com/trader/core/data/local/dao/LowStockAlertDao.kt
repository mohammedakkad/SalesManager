package com.trader.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trader.core.data.local.entity.LowStockAlertStateEntity

@Dao
interface LowStockAlertDao {
    @Query("SELECT * FROM low_stock_alert_states")
    suspend fun getAll(): List<LowStockAlertStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<LowStockAlertStateEntity>)

    @Query(
        """
        UPDATE low_stock_alert_states
        SET lastStatus = :status, lastNotifiedAt = :notifiedAt
        WHERE unitId = :unitId
        """
    )
    suspend fun markNotified(unitId: String, status: String, notifiedAt: Long)
}
