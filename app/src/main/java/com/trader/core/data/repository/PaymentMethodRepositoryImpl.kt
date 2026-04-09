package com.trader.core.data.repository

import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.entity.PaymentMethodEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PaymentMethodRepositoryImpl(
    private val dao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : PaymentMethodRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
        syncScope.launch {
            val code = activationRepo.getMerchantCode()
            if (code.isEmpty()) return@launch
            sync.observePaymentMethods(code).collect { list ->
                list.forEach { dao.insertPaymentMethod(PaymentMethodEntity.fromDomain(it)) }
            }
        }
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> =
        dao.getAllPaymentMethods().map { it.map(PaymentMethodEntity::toDomain) }

    override suspend fun getPaymentMethodById(id: Long) = dao.getPaymentMethodById(id)?.toDomain()

    override suspend fun insertPaymentMethod(m: PaymentMethod): Long {
        val id = dao.insertPaymentMethod(PaymentMethodEntity.fromDomain(m))
        sync.pushPaymentMethod(code(), m.copy(id = id))
        return id
    }

    override suspend fun updatePaymentMethod(m: PaymentMethod) {
        dao.updatePaymentMethod(PaymentMethodEntity.fromDomain(m))
        sync.pushPaymentMethod(code(), m)
    }

    override suspend fun deletePaymentMethod(m: PaymentMethod) {
        dao.deletePaymentMethod(PaymentMethodEntity.fromDomain(m))
        sync.deletePaymentMethod(code(), m.id)
    }
}
