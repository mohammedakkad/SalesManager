package com.trader.salesmanager.ui.debts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.local.appDataStore
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.salesmanager.ui.settings.STORE_NAME_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemindableDebt(
    val customer: Customer,
    val amount: Double,
    val nearestDueDate: Long,
    val transactions: List<Transaction>
)

data class RemindAllUiState(
    val debts: List<RemindableDebt> = emptyList(),
    val sentCustomerIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val storeName: String = ""
)

class RemindAllViewModel(
    private val customerRepo: CustomerRepository,
    private val transactionRepo: TransactionRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindAllUiState())
    val uiState: StateFlow<RemindAllUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val storeNameFlow = context.appDataStore.data.map { it[STORE_NAME_KEY] ?: "" }
            combine(
                transactionRepo.getDebtsDueTodayOrOverdue(),
                customerRepo.getAllCustomers(),
                storeNameFlow
            ) { dueTransactions, customers, storeName ->
                val customerMap = customers.associateBy { it.id }
                val grouped = dueTransactions
                    .groupBy { it.customerId }
                    .mapNotNull { (customerId, txs) ->
                        val customer = customerMap[customerId] ?: return@mapNotNull null
                        RemindableDebt(
                            customer = customer,
                            amount = txs.sumOf { it.amount },
                            nearestDueDate = txs.minOf { it.dueDate!! },
                            transactions = txs
                        )
                    }
                    .sortedBy { it.nearestDueDate }
                Triple(grouped, storeName, Unit)
            }.collect { (grouped, storeName, _) ->
                _uiState.update {
                    it.copy(
                        debts = grouped,
                        isLoading = false,
                        storeName = storeName
                    )
                }
            }
        }
    }

    fun markSent(customerId: Long) {
        _uiState.update { it.copy(sentCustomerIds = it.sentCustomerIds + customerId) }
    }
}
