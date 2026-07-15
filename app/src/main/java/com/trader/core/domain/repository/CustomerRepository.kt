package com.trader.core.domain.repository

import com.trader.core.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    fun searchCustomers(query: String): Flow<List<Customer>>
    suspend fun getCustomerById(id: Long): Customer?
    suspend fun insertCustomer(customer: Customer): Long
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer)
    suspend fun getPhoneConflict(phone: String, excludeId: Long = -999L): String?
    suspend fun getTransactionCount(customerId: Long): Int
    }