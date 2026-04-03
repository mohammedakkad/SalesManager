package com.trader.salesmanager.data.repository

import com.trader.salesmanager.data.local.dao.PaymentMethodDao
import com.trader.salesmanager.data.local.entity.PaymentMethodEntity
import com.trader.salesmanager.domain.model.PaymentMethod
import com.trader.salesmanager.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentMethodRepositoryImpl(private val dao: PaymentMethodDao) : PaymentMethodRepository {
    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> = dao.getAllPaymentMethods().map { it.map(PaymentMethodEntity::toDomain) }
    override suspend fun getPaymentMethodById(id: Long): PaymentMethod? = dao.getPaymentMethodById(id)?.toDomain()
    override suspend fun insertPaymentMethod(method: PaymentMethod): Long = dao.insertPaymentMethod(PaymentMethodEntity.fromDomain(method))
    override suspend fun updatePaymentMethod(method: PaymentMethod) = dao.updatePaymentMethod(PaymentMethodEntity.fromDomain(method))
    override suspend fun deletePaymentMethod(method: PaymentMethod) = dao.deletePaymentMethod(PaymentMethodEntity.fromDomain(method))
}