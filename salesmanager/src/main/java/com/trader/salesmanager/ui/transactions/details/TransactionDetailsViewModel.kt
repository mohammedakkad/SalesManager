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
        // ✅ مشكلة 5: Flow مستمر — يُحدِّث الشاشة فوراً عند أي تغيير في العملية
        viewModelScope.launch {
            transactionRepo.observeTransactionById(transactionId).collect {
                t ->
                _state.update {
                    it.copy(transaction = t)
                }
            }
        }
        viewModelScope.launch {
            invoiceItemRepo.getItemsForTransaction(transactionId).collect {
                items ->
                _state.update {
                    current ->
                    // ✅ ترتيب ثابت بـ productName → لا إعادة ترتيب بعد الحفظ
                    // ✅ تجاهل القائمة الفارغة اللحظية أثناء دورة delete+reinsert
                    //    (تحدث عندما يحذف saveItemsAndDeductStock القديم ويُدرج الجديد)
                    val sorted = items.sortedBy {
                        it.productName
                    }
                    if (sorted.isEmpty() && current.invoiceItems.isNotEmpty()
                        && current.transaction?.hasItems == true) {
                        current // لا تُحدِّث — هذه فترة عابرة
                    } else {
                        current.copy(invoiceItems = sorted)
                    }
                }
            }
        }
    }

    fun delete() {
        val t = _state.value.transaction ?: return
        viewModelScope.launch {
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