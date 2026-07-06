package com.trader.salesmanager.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.repository.CashBoxRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoxesUiState(
    val boxes: List<CashBox> = emptyList(),
    /** paymentMethodId → نوع الطريقة، لاختيار الأيقونة واللون */
    val paymentTypes: Map<Long, PaymentType> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /** مجموع أرصدة الصناديق المهيّأة فقط */
    val totalBalance: Double get() = boxes.filter { it.isInitialized }.sumOf { it.currentBalance }
    val initializedCount: Int get() = boxes.count { it.isInitialized }
}

class BoxesViewModel(
    private val cashBoxRepo: CashBoxRepository,
    paymentMethodRepo: PaymentMethodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoxesUiState())
    val uiState: StateFlow<BoxesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cashBoxRepo.getAllBoxes(),
                paymentMethodRepo.getAllPaymentMethods()
            ) { boxes, methods ->
                Pair(boxes, methods.associate { it.id to it.type })
            }.collect { (boxes, types) ->
                _uiState.update {
                    it.copy(boxes = boxes, paymentTypes = types, isLoading = false)
                }
            }
        }
        // إعادة رفع أي صندوق معلق عند فتح الشاشة
        viewModelScope.launch {
            runCatching { cashBoxRepo.syncPendingBoxes() }
        }
    }

    fun setInitialBalance(boxId: String, amountText: String) {
        val amount = amountText.toLatinDigits().toDoubleOrNull()
        if (amount == null || amount < 0) {
            _uiState.update {
                it.copy(error = "أدخل مبلغاً صحيحاً")
            }
            return
        }
        viewModelScope.launch {
            try {
                cashBoxRepo.setInitialBalance(boxId, amount)
                _uiState.update {
                    it.copy(error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "حدث خطأ: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update {
        it.copy(error = null)
    }
}

private fun String.toLatinDigits(): String = this
    .replace('٠', '0').replace('١', '1').replace('٢', '2')
    .replace('٣', '3').replace('٤', '4').replace('٥', '5')
    .replace('٦', '6').replace('٧', '7').replace('٨', '8')
    .replace('٩', '9').replace('۰', '0').replace('۱', '1')
    .replace('۲', '2').replace('۳', '3').replace('۴', '4')
    .replace('۵', '5').replace('۶', '6').replace('۷', '7')
    .replace('۸', '8').replace('۹', '9')
