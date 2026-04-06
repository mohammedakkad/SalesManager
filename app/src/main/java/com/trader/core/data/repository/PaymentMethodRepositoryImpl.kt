package com.trader.core.data.repository

import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.entity.PaymentMethodEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentMethodRepositoryImpl(
    private val dao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : PaymentMethodRepository {

    private suspend fun code() = activationRepo.getMerchantCode()

    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> =
    dao.getAllPaymentMethods().map {
        it.map(PaymentMethodEntity::toDomain)
    }

    override suspend fun getPaymentMethodById(id: Long): PaymentMethod? =
    dao.getPaymentMethodById(id)?.toDomain()

    override suspend fun insertPaymentMethod(method: PaymentMethod): Long {
        val id = dao.insertPaymentMethod(PaymentMethodEntity.fromDomain(method))
        sync.pushPaymentMethod(code(), method.copy(id = id))
        return id
    }

    override suspend fun updatePaymentMethod(method: PaymentMethod) {
        dao.updatePaymentMethod(PaymentMethodEntity.fromDomain(method))
        sync.pushPaymentMethod(code(), method)
    }

    override suspend fun deletePaymentMethod(method: PaymentMethod) {
        dao.deletePaymentMethod(PaymentMethodEntity.fromDomain(method))
        sync.deletePaymentMethod(code(), method.id)
    }
}