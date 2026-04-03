package com.trader.salesmanager.domain.repository

import com.trader.salesmanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>>
    fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getUnpaidTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double
    suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double
    suspend fun getUnpaidAmountByCustomer(customerId: Long): Double
}