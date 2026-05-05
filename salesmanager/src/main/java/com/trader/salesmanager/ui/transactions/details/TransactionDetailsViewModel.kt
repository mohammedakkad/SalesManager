package com.trader.salesmanager.ui.transactions.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.repository.ReturnRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
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
    private val stockRepo: StockRepository,
    private val returnRepo: ReturnRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsUiState())
    val uiState: StateFlow<TransactionDetailsUiState> = _state.asStateFlow()

    init {
        // ── Flow 1: العملية — يُحدَّث عند أي تغيير في DB ────────────
        viewModelScope.launch {
            transactionRepo.observeTransactionById(transactionId).collect {
                t ->
                _state.update {
                    it.copy(transaction = t)
                }
            }
        }

        // ── Flow 2: الأصناف + الإرجاعات — مدمجان لضمان التحديث الفوري ─
        // يُعيد حساب returnSummary تلقائياً عند:
        // 1. إضافة فاتورة إرجاع جديدة
        // 2. تعديل الأصناف
        viewModelScope.launch {
            combine(
                invoiceItemRepo.getItemsForTransaction(transactionId),
                returnRepo.getReturnsByTransaction(transactionId)
            ) {
                items, returns ->
                Pair(items, returns)
            }.collect {
                (items, _) ->
                // ✅ استقرار القائمة: تجاهل الفارغة اللحظية
                val sorted = items.sortedBy {
                    it.productName
                }
                _state.update {
                    current ->
                    if (sorted.isEmpty() && current.invoiceItems.isNotEmpty()
                        && current.transaction?.hasItems == true) current
                    else current.copy(invoiceItems = sorted)
                }
                // ✅ إعادة حساب ReturnSummary من DB عند أي تغيير
                if (items.isNotEmpty()) {
                    val summary = returnRepo.getReturnSummary(transactionId, items)
                    _state.update {
                        it.copy(returnSummary = summary)
                    }
                }
            }
        }
    }

    // تُستدعى من الـ Screen عند العودة من ReturnProcessScreen
    fun refreshReturnSummary() {
        val items = _state.value.invoiceItems
        if (items.isEmpty()) return
        viewModelScope.launch {
            val summary = returnRepo.getReturnSummary(transactionId, items)
            _state.update {
                it.copy(returnSummary = summary)
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