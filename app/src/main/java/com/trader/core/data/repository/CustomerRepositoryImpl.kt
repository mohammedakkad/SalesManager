package com.trader.core.data.repository

import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import com.trader.core.domain.model.SyncStatus

class CustomerRepositoryImpl(
    private val dao: CustomerDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CustomerRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
            .filter {
                it.isNotEmpty()
            }
            .distinctUntilChanged()
            .collectLatest {
                code ->
                sync.observeCustomers(code).collect {
                    list ->
                    list.forEach {
                        if (it.id != -1L) dao.upsertCustomer(CustomerEntity.fromDomain(it))
                    }
                }
            }
        }
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    override fun getAllCustomers() =
    dao.getAllCustomers().map {
        it.map(CustomerEntity::toDomain)
    }

    override fun searchCustomers(q: String) =
    dao.searchCustomers(q).map {
        it.map(CustomerEntity::toDomain)
    }

    override suspend fun getCustomerById(id: Long) = dao.getCustomerById(id)?.toDomain()

    // ✅ مشكلة 1: التحقق من تكرار الهاتف
    override suspend fun getPhoneConflict(phone: String, excludeId: Long): String? {
        if (phone.isBlank()) return null
        return dao.getByPhone(phone, excludeId)?.name
    }

    // ✅ مشكلة 3: عدد العمليات المرتبطة
    override suspend fun getTransactionCount(customerId: Long): Int =
    dao.getTransactionCount(customerId)

    override suspend fun insertCustomer(c: Customer): Long {
        val entity = CustomerEntity.fromDomain(c.copy(syncStatus = SyncStatus.PENDING))
        val id = dao.insertCustomer(entity)
        syncScope.launch {
            try {
                sync.pushCustomer(code(), c.copy(id = id))
                dao.markCustomerSynced(id)
            } catch (_: Exception) {}
        }
        return id
    }

    override suspend fun updateCustomer(c: Customer) {
        dao.updateCustomer(CustomerEntity.fromDomain(c.copy(syncStatus = SyncStatus.PENDING)))
        syncScope.launch {
            try {
                sync.pushCustomer(code(), c)
                dao.markCustomerSynced(c.id)
            } catch (_: Exception) {}
        }
    }

    override suspend fun deleteCustomer(c: Customer) {
        if (c.id == -1L) return
        dao.deleteCustomer(CustomerEntity.fromDomain(c))
        syncScope.launch {
            try {
                sync.deleteCustomer(code(), c.id)
            } catch (_: Exception) {}
        }
    }
}