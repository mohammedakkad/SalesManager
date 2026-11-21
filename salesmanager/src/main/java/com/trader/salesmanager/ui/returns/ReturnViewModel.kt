package com.trader.salesmanager.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.ReturnInvoice
import com.trader.core.domain.model.ReturnItem
import com.trader.core.domain.model.ReturnType
import com.trader.core.domain.model.ReturnUiState
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.ReturnRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── حالة كل صنف في شاشة الإرجاع ────────────────────────────────
data class ReturnLineState(
    val invoiceItem: InvoiceItem,
    val productName: String,
    val unitLabel: String,
    val costPrice: Double = 0.0,
    val alreadyReturned: Double = 0.0, // مجموع ما أُرجع سابقاً
    val returnQty: Double = 0.0, // الكمية التي يريد إرجاعها الآن
    val maxReturnable: Double =
    (invoiceItem.quantity - alreadyReturned).coerceAtLeast(0.0),
    val isSelected: Boolean = false // هل اختاره للإرجاع
) {
    val canReturn: Boolean  get() = maxReturnable.isFinite() && maxReturnable > 0
    val isValid: Boolean get() = invoiceItem.quantity.isFinite() && invoiceItem.quantity > 0 &&
        invoiceItem.pricePerUnit.isFinite() && invoiceItem.pricePerUnit >= 0 &&
        costPrice.isFinite() && costPrice >= 0 && alreadyReturned.isFinite() && alreadyReturned >= 0 &&
        maxReturnable.isFinite() && returnQty.isFinite() && returnQty >= 0 && returnQty <= maxReturnable &&
        refundAmount.isFinite() && lostProfit.isFinite()
    val refundAmount: Double get() = returnQty * invoiceItem.pricePerUnit
    val lostProfit: Double
    get() = if (costPrice > 0) returnQty * (invoiceItem.pricePerUnit - costPrice) else 0.0
}

data class ReturnScreenState(
    val lines: List<ReturnLineState> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = true,
    val processingState: ReturnUiState = ReturnUiState.Idle,
    val showConfirmSheet: Boolean = false,
    val loadError: String? = null
) {
    val selectedLines: List<ReturnLineState> get() = lines.filter {
        it.isSelected && it.returnQty > 0
    }
    val totalRefund: Double get() = selectedLines.sumOf {
        it.refundAmount
    }
    val totalLostProfit: Double get() = selectedLines.sumOf {
        it.lostProfit
    }
    val isFullReturn: Boolean get() = lines.all {
        !it.canReturn || (it.isSelected && it.returnQty == it.maxReturnable)
    }
    val canConfirm: Boolean get() = !isLoading && loadError == null && lines.all { it.isValid } &&
        selectedLines.isNotEmpty() && totalRefund.isFinite() && totalLostProfit.isFinite() &&
        processingState == ReturnUiState.Idle
}

class ReturnViewModel(
    private val returnRepo: ReturnRepository,
    private val invoiceRepo: InvoiceItemRepository,
    private val productRepo: ProductRepository,
    private val merchantId: String,
    private val transactionId: Long         // ✅ يأتي من Koin parametersOf
) : ViewModel() {

    private val _state = MutableStateFlow(ReturnScreenState())
    val state: StateFlow<ReturnScreenState> = _state.asStateFlow()
    private var isSubmitting = false
    // Keep identities and timestamps for an unchanged retry after uncertain completion.
    // This is in-memory retry protection, not OS process-restoration qualification.
    private var pendingRequest: Pair<ReturnInvoice, List<ReturnItem>>? = null

    init {
        load()
    } // ✅ يُستدعى تلقائياً عند الإنشاء

    private fun load() {
        _state.update { it.copy(isLoading = true, loadError = null, processingState = ReturnUiState.Idle) }
        viewModelScope.launch {
            try {
                val items = invoiceRepo.getItemsForTransactionOnce(transactionId)
                val lines = items.map { item ->
                    // A failed read is unknown, never evidence that no quantity was returned.
                    val alreadyReturned = returnRepo.getAlreadyReturnedQty(transactionId, item.unitId)
                    val unit = productRepo.getProductById(item.productId)?.units?.firstOrNull { it.id == item.unitId }
                    val maxReturnable = (item.quantity - alreadyReturned).coerceAtLeast(0.0)
                    ReturnLineState(
                        invoiceItem = item,
                        productName = item.productName,
                        unitLabel = item.unitLabel,
                        costPrice = unit?.costPrice ?: 0.0,
                        alreadyReturned = alreadyReturned,
                        returnQty = maxReturnable,
                        isSelected = maxReturnable > 0
                    )
                }
                require(lines.all { it.isValid } && lines.sumOf { it.refundAmount }.isFinite() &&
                    lines.sumOf { it.lostProfit }.isFinite()) { "Invalid return amounts" }
                _state.update { it.copy(lines = lines, isLoading = false, loadError = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val message = "تعذر تحميل بيانات الإرجاع، حاول مجددًا"
                _state.update {
                    it.copy(lines = emptyList(), loadError = message, processingState = ReturnUiState.Error(message))
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun retryLoad() {
        if (_state.value.loadError != null && !_state.value.isLoading && !isSubmitting) load()
    }

    private fun canEdit() = !isSubmitting && _state.value.processingState !is ReturnUiState.Success

    // ── Toggle اختيار صنف ────────────────────────────────────────
    fun toggleLine(index: Int) {
        if (!canEdit() || index !in _state.value.lines.indices) return
        pendingRequest = null
        _state.update {
            s ->
            s.copy(lines = s.lines.mapIndexed {
                i, line ->
                if (i == index) line.copy(isSelected = !line.isSelected) else line
            })
        }
    }

    // ── تغيير الكمية (Stepper) ───────────────────────────────────
    fun updateQty(index: Int, newQty: Double) {
        if (!canEdit() || !newQty.isFinite() || index !in _state.value.lines.indices) return
        val current = _state.value.lines[index]
        val quantity = newQty.coerceIn(0.0, current.maxReturnable)
        if (quantity == current.returnQty && current.isSelected == (quantity > 0)) return
        pendingRequest = null
        _state.update {
            s ->
            s.copy(lines = s.lines.mapIndexed {
                i, line ->
                if (i == index) {
                    val clamped = newQty.coerceIn(0.0, line.maxReturnable)
                    line.copy(returnQty = clamped, isSelected = clamped > 0)
                } else line
            })
        }
    }

    fun updateNote(note: String) {
        if (!canEdit() || note == _state.value.note) return
        pendingRequest = null
        _state.update { it.copy(note = note) }
    }

    fun showConfirmSheet() {
        if (_state.value.canConfirm) _state.update { it.copy(showConfirmSheet = true) }
    }
    fun dismissConfirmSheet() {
        if (!canEdit()) return
        _state.update { it.copy(showConfirmSheet = false,
            processingState = if (it.loadError == null) ReturnUiState.Idle else it.processingState) }
    }

    // ── تأكيد الإرجاع ────────────────────────────────────────────
    fun confirmReturn() {
        val s = _state.value
        if (isSubmitting || !s.canConfirm) return
        if (merchantId.isBlank()) {
            _state.update { it.copy(processingState = ReturnUiState.Error("انتهت جلسة التاجر"), showConfirmSheet = false) }
            return
        }

        val invoice = pendingRequest?.first ?: ReturnInvoice(
            originalTransactionId = transactionId,
            merchantId = merchantId,
            returnType = if (s.isFullReturn) ReturnType.FULL else ReturnType.PARTIAL,
            totalRefund = s.totalRefund,
            note = s.note
        )
        val returnItems = pendingRequest?.second ?: s.selectedLines.map { line ->
            ReturnItem(
                returnInvoiceId = invoice.id,
                productId = line.invoiceItem.productId,
                productName = line.productName,
                unitId = line.invoiceItem.unitId,
                unitLabel = line.unitLabel,
                originalQuantity = line.invoiceItem.quantity,
                returnedQuantity = line.returnQty,
                pricePerUnit = line.invoiceItem.pricePerUnit,
                costPricePerUnit = line.costPrice
            )
        }
        pendingRequest = invoice to returnItems
        isSubmitting = true
        _state.update { it.copy(processingState = ReturnUiState.Loading) }

        viewModelScope.launch {
            try {
                val result = returnRepo.processReturn(invoice, returnItems)
                _state.update { it.copy(processingState = ReturnUiState.Success(result), showConfirmSheet = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val message = when (failure) {
                    is IllegalArgumentException -> failure.message ?: "بيانات غير صحيحة"
                    is IllegalStateException -> failure.message ?: "تجاوز الكمية المسموح بها"
                    else -> "حدث خطأ، حاول مجدداً"
                }
                _state.update { it.copy(processingState = ReturnUiState.Error(message), showConfirmSheet = false) }
            } finally {
                isSubmitting = false
                _state.update {
                    if (it.processingState == ReturnUiState.Loading)
                        it.copy(processingState = ReturnUiState.Idle, showConfirmSheet = false) else it
                }
            }
        }
    }
}
