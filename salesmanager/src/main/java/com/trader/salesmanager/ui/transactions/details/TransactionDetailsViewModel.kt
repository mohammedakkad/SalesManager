package com.trader.salesmanager.ui.transactions.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.ReturnRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.salesmanager.util.InvoiceItemsGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionDetailsUiState(
    val transaction: Transaction? = null,
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val isDeleted: Boolean = false,
    val returnSummary: ReturnSummary = ReturnSummary.NONE,
    val isLoadingReturn: Boolean = false
)

class TransactionDetailsViewModel(
    private val transactionId: Long,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val returnRepo: ReturnRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsUiState())
    val uiState: StateFlow<TransactionDetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepo.observeTransactionById(transactionId).collect { t ->
                _state.update { it.copy(transaction = t) }
            }
        }

        viewModelScope.launch {
            combine(
                invoiceItemRepo.getItemsForTransaction(transactionId),
                returnRepo.getReturnsByTransaction(transactionId),
                _state
            ) { items, returns, current ->
                Triple(items, returns, current.transaction)
            }.collect { (items, _, transaction) ->
                val validated = InvoiceItemsGuard.filter(items, transaction)
                val sorted = validated.sortedBy { it.productName }
                _state.update { current ->
                    if (sorted.isEmpty() && current.invoiceItems.isNotEmpty()
                        && current.transaction?.hasItems == true
                    ) {
                        current
                    } else {
                        current.copy(invoiceItems = sorted)
                    }
                }
                if (validated.isNotEmpty()) {
                    val summary = returnRepo.getReturnSummary(transactionId, validated)
                    _state.update { it.copy(returnSummary = summary) }
                } else {
                    _state.update { it.copy(returnSummary = ReturnSummary.NONE) }
                }
            }
        }
    }

    fun refreshReturnSummary() {
        val items = _state.value.invoiceItems
        if (items.isEmpty()) return
        viewModelScope.launch {
            val summary = returnRepo.getReturnSummary(transactionId, items)
            _state.update { it.copy(returnSummary = summary) }
        }
    }

    fun delete() {
        val t = _state.value.transaction ?: return
        viewModelScope.launch {
            transactionRepo.deleteTransaction(t)
            _state.update { it.copy(isDeleted = true) }
        }
    }
}
