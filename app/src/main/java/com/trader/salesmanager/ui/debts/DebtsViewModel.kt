package com.trader.salesmanager.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.model.Customer
import com.trader.salesmanager.domain.repository.CustomerRepository
import com.trader.salesmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerDebt(val customer: Customer, val debt: Double)
data class DebtsUiState(val debts: List<CustomerDebt> = emptyList(), val isLoading: Boolean = true)

class DebtsViewModel(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DebtsUiState())
    val uiState: StateFlow<DebtsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customerRepo.getAllCustomers().collect { customers ->
                val debts = customers.mapNotNull { customer ->
                    val debt = transactionRepo.getUnpaidAmountByCustomer(customer.id)
                    if (debt > 0) CustomerDebt(customer, debt) else null
                }.sortedByDescending { it.debt }
                _uiState.value = DebtsUiState(debts = debts, isLoading = false)
            }
        }
    }
}