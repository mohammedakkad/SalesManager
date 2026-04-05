package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>
    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getPaymentMethodById(id: Long): PaymentMethodEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long
    @Update suspend fun updatePaymentMethod(method: PaymentMethodEntity)
    @Delete suspend fun deletePaymentMethod(method: PaymentMethodEntity)
}
