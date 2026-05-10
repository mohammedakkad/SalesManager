package com.trader.core.data.manager

import com.trader.core.domain.model.Employee
import com.trader.core.domain.repository.EmployeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmployeeSessionManager(
    private val employeeRepository: EmployeeRepository
) {
    private val _currentActiveEmployee = MutableStateFlow<Employee?>(null)
    val currentActiveEmployee: StateFlow<Employee?> = _currentActiveEmployee.asStateFlow()

    val isLoggedIn: Boolean
        get() = _currentActiveEmployee.value != null

    suspend fun loginWithPin(pin: String): Boolean {
        val employee = employeeRepository.getByPin(pin) ?: return false
        _currentActiveEmployee.value = employee
        return true
    }

    fun logout() {
        _currentActiveEmployee.value = null
    }
}
