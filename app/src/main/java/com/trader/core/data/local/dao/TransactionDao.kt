package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsByCustomer(customerId: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isPaid = 0 ORDER BY date ASC")
    fun getUnpaidTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Update suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isPaid = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isPaid = 0 AND customerId = :customerId")
    suspend fun getUnpaidAmountByCustomer(customerId: Long): Double
    @Query("DELETE FROM transactions") suspend fun deleteAll()
}
