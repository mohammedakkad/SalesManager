package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.SaleOperationEntity

@Dao
interface SaleOperationDao {
    @Query("SELECT * FROM sale_operations WHERE transactionId = :id")
    suspend fun forTransaction(id: Long): List<SaleOperationEntity>
    @Query("SELECT * FROM sale_operations WHERE merchantId = :merchantId")
    suspend fun allForMerchant(merchantId: String): List<SaleOperationEntity>
    @Query("DELETE FROM sale_operations")
    suspend fun deleteAll()

    @Query("SELECT * FROM sale_operations WHERE merchantId = :merchantId AND operationId = :operationId")
    suspend fun get(merchantId: String, operationId: String): SaleOperationEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: SaleOperationEntity)
}
