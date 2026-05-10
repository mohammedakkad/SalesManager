package com.trader.salesmanager.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.ReturnRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

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
    val canReturn: Boolean  get() = maxReturnable > 0
    val refundAmount: Double get() = returnQty * invoiceItem.pricePerUnit
    val lostProfit: Double
    get() = if (costPrice > 0) returnQty * (invoiceItem.pricePerUnit - costPrice) else 0.0
}

data class ReturnScreenState(
    val lines: List<ReturnLineState> = emptyList(),
    val note: String = "",
    val isLoading: Boolean = true,
    val isPartialEnabled: Boolean = false,
    val processingState: ReturnUiState = ReturnUiState.Idle,
    val showConfirmSheet: Boolean = false
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
        !it.canReturn || it.returnQty == it.maxReturnable
    }
    val canConfirm: Boolean get() = selectedLines.isNotEmpty() && processingState == ReturnUiState.Idle
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

    init {
        load()
    } // ✅ يُستدعى تلقائياً عند الإنشاء

    private fun load() {
        val transactionId = this.transactionId
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }

            val isPartialEnabled = returnRepo.isPartialReturnEnabled()
            val items = invoiceRepo.getItemsForTransactionOnce(transactionId)

            val lines = items.map {
                item ->
                // ✅ إصلاح Bug 1: جلب الكمية المُرجَعة فعلاً من DB بدل 0.0 hardcoded
                val alreadyReturned = runCatching {
                    returnRepo.getAlreadyReturnedQty(transactionId, item.unitId)
                }.getOrDefault(0.0)

                val unit = productRepo.getProductById(item.productId)
                ?.units?.firstOrNull {
                    it.id == item.unitId
                }

                val maxReturnable = (item.quantity - alreadyReturned).coerceAtLeast(0.0)

                ReturnLineState(
                    invoiceItem = item,
                    productName = item.productName,
                    unitLabel = item.unitLabel,
                    costPrice = unit?.costPrice ?: 0.0,
                    alreadyReturned = alreadyReturned,
                    returnQty = maxReturnable, // ✅ بداية بالكمية المتبقية الفعلية
                    isSelected = maxReturnable > 0 // ✅ فقط إذا لا يزال هناك ما يُرجع
                )
            }

            _state.update {
                it.copy(
                    lines = lines,
                    isPartialEnabled = isPartialEnabled,
                    isLoading = false
                )
            }
        }
    }

    // ── Toggle اختيار صنف ────────────────────────────────────────
    fun toggleLine(index: Int) {
        val current = _state.value.lines[index]

        // Test Case 3: منع الجزئي على الخطة المجانية
        val isBecomingPartial = !current.isSelected &&
        _state.value.lines.any {
            it.isSelected
        }
        if (isBecomingPartial && !_state.value.isPartialEnabled) {
            _state.update {
                it.copy(processingState = ReturnUiState.PartialReturnLocked)
            }
            return
        }

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

    fun updateNote(note: String) = _state.update {
        it.copy(note = note)
    }

    fun showConfirmSheet() = _state.update {
        it.copy(showConfirmSheet = true)
    }
    fun dismissConfirmSheet() = _state.update {
        it.copy(showConfirmSheet = false, processingState = ReturnUiState.Idle)
    }

    // ── تأكيد الإرجاع ────────────────────────────────────────────
    fun confirmReturn() {
        val s = _state.value
        if (!s.canConfirm) return

        viewModelScope.launch {
            _state.update {
                it.copy(processingState = ReturnUiState.Loading)
            }

            val returnType = if (s.isFullReturn) ReturnType.FULL else ReturnType.PARTIAL
            val invoice = ReturnInvoice(
                originalTransactionId = this@ReturnViewModel.transactionId,
                merchantId = merchantId,
                returnType = returnType,
                totalRefund = s.totalRefund,
                note = s.note
            )
            val returnItems = s.selectedLines.map {
                line ->
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

            // ✅ Local-First: الحفظ المحلي يكتمل دائماً حتى offline
            // Firebase يحاول في الخلفية بدون انتظار (لن يعلق UI)
            runCatching {
                returnRepo.processReturn(invoice, returnItems)
            }.fold(
                onSuccess = {
                    result ->
                    _state.update {
                        it.copy(
                            processingState = ReturnUiState.Success(result),
                            showConfirmSheet = false
                        )
                    }
                },
                onFailure = {
                    e ->
                    _state.update {
                        it.copy(
                            processingState = ReturnUiState.Error(
                                when (e) {
                                    is IllegalArgumentException -> e.message ?: "بيانات غير صحيحة"
                                    is IllegalStateException -> e.message ?: "تجاوز الكمية المسموح بها"
                                    else -> "حدث خطأ، حاول مجدداً"
                                }
                            ),
                            showConfirmSheet = false
                        )
                    }
                }
            )
        }
    }
}