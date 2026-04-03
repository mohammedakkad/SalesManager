package com.trader.salesmanager.domain.repository

import com.trader.salesmanager.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun getPaymentMethodById(id: Long): PaymentMethod?
    suspend fun insertPaymentMethod(method: PaymentMethod): Long
    suspend fun updatePaymentMethod(method: PaymentMethod)
    suspend fun deletePaymentMethod(method: PaymentMethod)
}