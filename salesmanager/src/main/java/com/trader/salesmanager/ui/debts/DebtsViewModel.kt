package com.trader.salesmanager.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerDebt(
    val customer: Customer,
    val debt: Double,
    val nearestDueDate: Long? = null,
    val dueTransactions: List<Transaction> = emptyList()
)

data class DebtsUiState(
    val debts: List<CustomerDebt> = emptyList(),
    val isLoading: Boolean = true,
    val hasDueReminders: Boolean = false
)

class DebtsViewModel(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DebtsUiState())
    val uiState: StateFlow<DebtsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                customerRepo.getAllCustomers(),
                transactionRepo.getUnpaidTransactions(),
                transactionRepo.getDebtsDueTodayOrOverdue()
            ) { customers, unpaidTransactions, dueTransactions ->
                val customerMap = customers.associateBy { it.id }
                val debtByCustomer = unpaidTransactions
                    .groupBy { it.customerId }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                val dueByCustomer = dueTransactions.groupBy { it.customerId }
                val debts = debtByCustomer.mapNotNull { (customerId, debt) ->
                    val customer = customerMap[customerId] ?: return@mapNotNull null
                    val customerDueTxs = dueByCustomer[customerId].orEmpty()
                    CustomerDebt(
                        customer = customer,
                        debt = debt,
                        nearestDueDate = customerDueTxs.minOfOrNull { it.dueDate!! },
                        dueTransactions = customerDueTxs
                    )
                }.sortedByDescending { it.debt }
                DebtsUiState(
                    debts = debts,
                    isLoading = false,
                    hasDueReminders = dueTransactions.isNotEmpty()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
