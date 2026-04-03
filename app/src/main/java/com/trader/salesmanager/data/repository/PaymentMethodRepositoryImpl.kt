package com.trader.salesmanager.data.repository

import com.trader.salesmanager.data.local.dao.PaymentMethodDao
import com.trader.salesmanager.data.local.entity.PaymentMethodEntity
import com.trader.salesmanager.data.remote.FirebaseSyncService
import com.trader.salesmanager.domain.model.PaymentMethod
import com.trader.salesmanager.domain.repository.ActivationRepository
import com.trader.salesmanager.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentMethodRepositoryImpl(
    private val dao: PaymentMethodDao,
    private val syncService: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : PaymentMethodRepository {

    private suspend fun merchantCode() = activationRepo.getMerchantCode()

    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> =
        dao.getAllPaymentMethods().map { it.map(PaymentMethodEntity::toDomain) }

    override suspend fun getPaymentMethodById(id: Long): PaymentMethod? =
        dao.getPaymentMethodById(id)?.toDomain()

    override suspend fun insertPaymentMethod(method: PaymentMethod): Long {
        val id = dao.insertPaymentMethod(PaymentMethodEntity.fromDomain(method))
        val saved = method.copy(id = id)
        syncService.pushPaymentMethod(merchantCode(), saved)
        return id
    }

    override suspend fun updatePaymentMethod(method: PaymentMethod) {
        dao.updatePaymentMethod(PaymentMethodEntity.fromDomain(method))
        syncService.pushPaymentMethod(merchantCode(), method)
    }

    override suspend fun deletePaymentMethod(method: PaymentMethod) {
        dao.deletePaymentMethod(PaymentMethodEntity.fromDomain(method))
        syncService.deletePaymentMethod(merchantCode(), method.id)
    }
}
