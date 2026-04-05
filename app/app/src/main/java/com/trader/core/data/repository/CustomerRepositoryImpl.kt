package com.trader.core.data.repository

import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val dao: CustomerDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CustomerRepository {
    private suspend fun code() = activationRepo.getMerchantCode()
    override fun getAllCustomers(): Flow<List<Customer>> = dao.getAllCustomers().map { it.map(CustomerEntity::toDomain) }
    override fun searchCustomers(q: String): Flow<List<Customer>> = dao.searchCustomers(q).map { it.map(CustomerEntity::toDomain) }
    override suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)?.toDomain()
    override suspend fun insertCustomer(customer: Customer): Long {
        val id = dao.insertCustomer(CustomerEntity.fromDomain(customer))
        sync.pushCustomer(code(), customer.copy(id = id)); return id
    }
    override suspend fun updateCustomer(customer: Customer) { dao.updateCustomer(CustomerEntity.fromDomain(customer)); sync.pushCustomer(code(), customer) }
    override suspend fun deleteCustomer(customer: Customer) { dao.deleteCustomer(CustomerEntity.fromDomain(customer)); sync.deleteCustomer(code(), customer.id) }
}
