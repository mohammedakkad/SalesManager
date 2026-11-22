package com.trader.core.domain.repository

import com.trader.core.domain.model.Employee
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {

    fun observeEmployees(): Flow<List<Employee>>

    suspend fun addOrUpdate(employee: Employee)

    suspend fun getById(id: String): Employee?

    suspend fun deleteById(id: String)

    suspend fun getByPin(pin: String): Employee?

    suspend fun ensureDefaultAdmin()
}
