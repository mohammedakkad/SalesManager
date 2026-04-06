package com.trader.salesmanager.ui.customers.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val transactions: List<Transaction> = emptyList(),
    val totalDebt: Double = 0.0,
    val isLoading: Boolean = true
)

class CustomerDetailsViewModel(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository,
    private val customerId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val customer = customerRepo.getCustomerById(customerId)
            transactionRepo.getTransactionsByCustomer(customerId).collect { transactions ->
                val debt = transactionRepo.getUnpaidAmountByCustomer(customerId)
                _uiState.update { it.copy(customer = customer, transactions = transactions, totalDebt = debt, isLoading = false) }
            }
        }
    }
}