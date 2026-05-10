package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity)

    @Query("""
        SELECT * FROM stock_movements 
        WHERE productId = :productId AND unitId = :unitId 
        ORDER BY createdAt DESC
    """)
    fun getForProductUnit(productId: String, unitId: String): Flow<List<StockMovementEntity>>

    @Query("""
        SELECT * FROM stock_movements 
        WHERE relatedTransactionId = :transactionId
        ORDER BY createdAt DESC
    """)
    fun getForTransaction(transactionId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<StockMovementEntity>

    @Query("UPDATE stock_movements SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}
