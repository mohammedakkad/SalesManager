package com.trader.salesmanager.ui.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionsViewModel(private val repo: TransactionRepository) : ViewModel() {

    private val _filterPaid = MutableStateFlow<Boolean?>(null)

    private val _transactions: Flow<List<Transaction>> = _filterPaid.flatMapLatest { filter ->
        when (filter) {
            null  -> repo.getAllTransactions()
            false -> repo.getUnpaidTransactions()
            true  -> repo.getAllTransactions().map { it.filter { t -> t.isPaid } }
        }
    }

    val uiState: StateFlow<TransactionsUiState> = combine(_transactions, _filterPaid) { list, filter ->
        TransactionsUiState(transactions = list, filterPaid = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setFilter(paid: Boolean?) { _filterPaid.value = paid }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repo.deleteTransaction(transaction) }
    }
}