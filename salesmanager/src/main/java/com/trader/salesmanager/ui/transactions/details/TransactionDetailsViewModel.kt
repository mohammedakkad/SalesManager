package com.trader.salesmanager.ui.transactions.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.ReturnRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionDetailsUiState(
    val transaction: Transaction? = null,
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val customer: Customer? = null,
    val priorDebtBalance: Double? = null,
    val currentDebtBalance: Double? = null,
    val isItemsLoaded: Boolean = false,
    val isDeleted: Boolean = false,
    val returnSummary: ReturnSummary = ReturnSummary.NONE,
    val isLoadingReturn: Boolean = false
)

class TransactionDetailsViewModel(
    private val transactionId: Long,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val stockRepo: StockRepository,
    private val returnRepo: ReturnRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsUiState())
    val uiState: StateFlow<TransactionDetailsUiState> = _state.asStateFlow()

    init {
        // ── Flow 1: العملية — يُحدَّث عند أي تغيير في DB ────────────
        viewModelScope.launch {
            transactionRepo.observeTransactionById(transactionId).collect {
                t ->
                val customer = t?.takeIf { it.customerId > 0L }?.let {
                    customerRepo.getCustomerById(it.customerId)
                }
                val priorDebt = t?.takeIf { it.customerId > 0L }?.let {
                    transactionRepo.getUnpaidAmountBeforeTransaction(
                        customerId = it.customerId,
                        transactionDate = it.date,
                        transactionId = it.id
                    )
                }
                val currentDebt = t?.takeIf { it.customerId > 0L }?.let {
                    transactionRepo.getUnpaidAmountByCustomer(it.customerId)
                }
                _state.update {
                    it.copy(
                        transaction = t,
                        customer = customer,
                        priorDebtBalance = priorDebt,
                        currentDebtBalance = currentDebt
                    )
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
                        && current.transaction?.hasItems == true
                    ) {
                        current.copy(isItemsLoaded = true)
                    } else {
                        current.copy(invoiceItems = sorted, isItemsLoaded = true)
                    }
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