package com.trader.salesmanager.data.repository

import com.trader.salesmanager.data.local.dao.CustomerDao
import com.trader.salesmanager.data.local.entity.CustomerEntity
import com.trader.salesmanager.domain.model.Customer
import com.trader.salesmanager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepositoryImpl(private val dao: CustomerDao) : CustomerRepository {
    override fun getAllCustomers(): Flow<List<Customer>> = dao.getAllCustomers().map { it.map(CustomerEntity::toDomain) }
    override fun searchCustomers(query: String): Flow<List<Customer>> = dao.searchCustomers(query).map { it.map(CustomerEntity::toDomain) }
    override suspend fun getCustomerById(id: Long): Customer? = dao.getCustomerById(id)?.toDomain()
    override suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(CustomerEntity.fromDomain(customer))
    override suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(CustomerEntity.fromDomain(customer))
    override suspend fun deleteCustomer(customer: Customer) = dao.deleteCustomer(CustomerEntity.fromDomain(customer))
}