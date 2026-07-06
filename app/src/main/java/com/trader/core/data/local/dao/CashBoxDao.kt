package com.trader.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trader.core.data.local.entity.CashBoxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashBoxDao {
    @Query("SELECT * FROM cash_boxes ORDER BY paymentMethodName ASC")
    fun getAll(): Flow<List<CashBoxEntity>>

    @Query("SELECT * FROM cash_boxes")
    suspend fun getAllOnce(): List<CashBoxEntity>

    @Query("SELECT * FROM cash_boxes WHERE id = :id")
    suspend fun getById(id: String): CashBoxEntity?

    @Query("SELECT * FROM cash_boxes WHERE paymentMethodId = :paymentMethodId")
    suspend fun getByPaymentMethodId(paymentMethodId: Long): CashBoxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(box: CashBoxEntity)

    @Query("UPDATE cash_boxes SET currentBalance = :newBalance, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Double, updatedAt: Long)

    /** تعديل تراكمي ذرّي — يمنع فقدان تحديثات متزامنة على نفس الصندوق */
    @Query("UPDATE cash_boxes SET currentBalance = currentBalance + :delta, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun applyDelta(id: String, delta: Double, updatedAt: Long)

    @Query("SELECT * FROM cash_boxes WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<CashBoxEntity>

    @Query("UPDATE cash_boxes SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM cash_boxes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cash_boxes")
    suspend fun deleteAll()
}
