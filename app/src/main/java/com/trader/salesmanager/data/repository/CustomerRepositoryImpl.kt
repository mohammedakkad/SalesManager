package com.trader.salesmanager.data.repository

import com.trader.salesmanager.data.local.dao.CustomerDao
import com.trader.salesmanager.data.local.entity.CustomerEntity
import com.trader.salesmanager.data.remote.FirebaseSyncService
import com.trader.salesmanager.domain.model.Customer
import com.trader.salesmanager.domain.repository.ActivationRepository
import com.trader.salesmanager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(
    private val dao: CustomerDao,
    private val syncService: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CustomerRepository {

    private suspend fun merchantCode() = activationRepo.getMerchantCode()

    override fun getAllCustomers(): Flow<List<Customer>> =
        dao.getAllCustomers().map { it.map(CustomerEntity::toDomain) }

    override fun searchCustomers(query: String): Flow<List<Customer>> =
        dao.searchCustomers(query).map { it.map(CustomerEntity::toDomain) }

    override suspend fun getCustomerById(id: Long): Customer? =
        dao.getCustomerById(id)?.toDomain()

    override suspend fun insertCustomer(customer: Customer): Long {
        val id = dao.insertCustomer(CustomerEntity.fromDomain(customer))
        val saved = customer.copy(id = id)
        syncService.pushCustomer(merchantCode(), saved)   // push to Firebase
        return id
    }

    override suspend fun updateCustomer(customer: Customer) {
        dao.updateCustomer(CustomerEntity.fromDomain(customer))
        syncService.pushCustomer(merchantCode(), customer) // push to Firebase
    }

    override suspend fun deleteCustomer(customer: Customer) {
        dao.deleteCustomer(CustomerEntity.fromDomain(customer))
        syncService.deleteCustomer(merchantCode(), customer.id) // delete from Firebase
    }
}
