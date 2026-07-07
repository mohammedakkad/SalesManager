package com.trader.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trader.core.data.local.entity.CashBoxMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashBoxMovementDao {
    @Query("SELECT * FROM cash_box_movements ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CashBoxMovementEntity>>

    @Query("SELECT * FROM cash_box_movements WHERE cashBoxId = :cashBoxId ORDER BY createdAt DESC")
    fun getByCashBoxId(cashBoxId: String): Flow<List<CashBoxMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: CashBoxMovementEntity)

    @Query("SELECT * FROM cash_box_movements WHERE id = :id")
    suspend fun getById(id: String): CashBoxMovementEntity?

    @Query("SELECT * FROM cash_box_movements WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<CashBoxMovementEntity>

    @Query("UPDATE cash_box_movements SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM cash_box_movements WHERE cashBoxId = :cashBoxId")
    suspend fun deleteByCashBoxId(cashBoxId: String)

    @Query("DELETE FROM cash_box_movements")
    suspend fun deleteAll()
}
