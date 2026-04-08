package com.trader.core.data.repository

import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val dao: CustomerDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CustomerRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Start realtime sync immediately when the repo is created (Koin singleton init)
    init { startRealtimeSync() }

    private fun startRealtimeSync() {
        syncScope.launch {
            val code = activationRepo.getMerchantCode()
            if (code.isEmpty()) return@launch
            sync.observeCustomers(code).collect { list ->
                list.forEach { dao.insertCustomer(CustomerEntity.fromDomain(it)) }
            }
        }
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    override fun getAllCustomers(): Flow<List<Customer>> =
        dao.getAllCustomers().map { it.map(CustomerEntity::toDomain) }

    override fun searchCustomers(q: String): Flow<List<Customer>> =
        dao.searchCustomers(q).map { it.map(CustomerEntity::toDomain) }

    override suspend fun getCustomerById(id: Long) = dao.getCustomerById(id)?.toDomain()

    override suspend fun insertCustomer(c: Customer): Long {
        val id = dao.insertCustomer(CustomerEntity.fromDomain(c))
        sync.pushCustomer(code(), c.copy(id = id))
        return id
    }

    override suspend fun updateCustomer(c: Customer) {
        dao.updateCustomer(CustomerEntity.fromDomain(c))
        sync.pushCustomer(code(), c)
    }

    override suspend fun deleteCustomer(c: Customer) {
        dao.deleteCustomer(CustomerEntity.fromDomain(c))
        sync.deleteCustomer(code(), c.id)
    }
}
