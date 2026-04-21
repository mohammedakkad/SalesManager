package com.trader.salesmanager.ui.transactions.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionDetailsUiState(
    val transaction: Transaction? = null,
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val isDeleted: Boolean = false
)

class TransactionDetailsViewModel(
    private val transactionId: Long,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val stockRepo: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsUiState())
    val uiState: StateFlow<TransactionDetailsUiState> = _state.asStateFlow()

    init {
        loadTransaction()
        observeInvoiceItems()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val t = transactionRepo.getTransactionById(transactionId)
            _state.update {
                it.copy(transaction = t)
            }
        }
    }

    private fun observeInvoiceItems() {
        viewModelScope.launch {
            invoiceItemRepo.getItemsForTransaction(transactionId).collect {
                items ->
                _state.update {
                    it.copy(invoiceItems = items)
                }
            }
        }
    }

    fun delete() {
        val t = _state.value.transaction ?: return
        viewModelScope.launch {
            // ✅ نستخدم invoiceItems المحملة فعلاً من الـ Flow
            // لا نعتمد على t.hasItems لأنه قد يكون خاطئاً في بيانات قديمة
            val items = _state.value.invoiceItems
            if (items.isNotEmpty()) {
                items.forEach {
                    item ->
                    stockRepo.returnStock(
                        productId = item.productId,
                        unitId = item.unitId,
                        quantity = item.quantity,
                        transactionId = t.id,
                        productName = item.productName,
                        unitLabel = item.unitLabel
                    )
                }
                invoiceItemRepo.deleteItemsForTransaction(t.id)
            }
            transactionRepo.deleteTransaction(t)
            _state.update {
                it.copy(isDeleted = true)
            }
        }
    }
}