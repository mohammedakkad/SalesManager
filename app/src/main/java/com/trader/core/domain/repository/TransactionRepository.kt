package com.trader.core.domain.repository

import com.trader.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>>
    fun getUnpaidTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    fun observeTransactionById(id: Long): Flow<Transaction?>   // ← مشكلة 5
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double
    suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double
    suspend fun getUnpaidAmountByCustomer(customerId: Long): Double
    fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<Transaction>>
    }