package com.trader.salesmanager.ui.boxes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.AdjustmentReason
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.CashBoxMovement
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.repository.CashBoxRepository
import com.trader.core.domain.repository.PaymentMethodRepository
import com.trader.salesmanager.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoxesUiState(
    val boxes: List<CashBox> = emptyList(),
    val movements: List<CashBoxMovement> = emptyList(),
    val paymentTypes: Map<Long, PaymentType> = emptyMap(),
    val expandedBoxId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false
) {
    val totalBalance: Double get() = boxes.filter { it.isInitialized }.sumOf { it.currentBalance }
    val initializedCount: Int get() = boxes.count { it.isInitialized }

    fun movementsForBox(boxId: String): List<CashBoxMovement> =
        movements.filter { it.cashBoxId == boxId }
}

class BoxesViewModel(
    private val cashBoxRepo: CashBoxRepository,
    paymentMethodRepo: PaymentMethodRepository,
    context: Context
) : ViewModel() {

    private val context = context.applicationContext

    private val _uiState = MutableStateFlow(BoxesUiState())
    val uiState: StateFlow<BoxesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cashBoxRepo.getAllBoxes(),
                paymentMethodRepo.getAllPaymentMethods(),
                cashBoxRepo.getAllMovements()
            ) { boxes, methods, movements ->
                Triple(boxes, methods.associate { it.id to it.type }, movements)
            }.collect { (boxes, types, movements) ->
                _uiState.update {
                    it.copy(
                        boxes = boxes,
                        paymentTypes = types,
                        movements = movements,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching { cashBoxRepo.syncPendingBoxes() }
        }
    }

    fun toggleBoxExpanded(boxId: String) {
        _uiState.update { state ->
            state.copy(
                expandedBoxId = if (state.expandedBoxId == boxId) null else boxId
            )
        }
    }

    fun setInitialBalance(boxId: String, amountText: String, onSaved: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        val amount = amountText.toLatinDigits().toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount < 0) {
            _uiState.update {
                it.copy(error = context.getString(R.string.boxes_error_invalid_amount))
            }
            return
        }
        submit(onSaved) { cashBoxRepo.setInitialBalance(boxId, amount) }
    }

    fun adjustBalance(
        boxId: String,
        amountText: String,
        note: String?,
        reason: AdjustmentReason,
        onSaved: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        val amount = amountText.toLatinDigits().toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount < 0) {
            _uiState.update {
                it.copy(error = context.getString(R.string.boxes_error_invalid_amount))
            }
            return
        }
        val reasonLabel = reasonLabel(reason)
        val fullNote = buildString {
            append(reasonLabel)
            if (!note.isNullOrBlank()) {
                append(": ")
                append(note.trim())
            }
        }
        submit(onSaved) { cashBoxRepo.adjustBalance(boxId, amount, fullNote, reason) }
    }

    private fun submit(onSaved: () -> Unit, action: suspend () -> Unit) {
        // Claim synchronously: another tap must observe the in-progress operation.
        while (true) {
            val current = _uiState.value
            if (current.isSaving) return
            if (_uiState.compareAndSet(current, current.copy(isSaving = true, error = null))) break
        }
        viewModelScope.launch {
            val saved = try {
                action()
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = context.getString(R.string.boxes_error_generic, e.message ?: ""))
                }
                false
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
            // UI observer failures must not be reported as a failed monetary mutation.
            if (saved) onSaved()
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun reasonLabel(reason: AdjustmentReason): String = when (reason) {
        AdjustmentReason.CORRECTION -> context.getString(R.string.boxes_reason_correction)
        AdjustmentReason.COUNT -> context.getString(R.string.boxes_reason_count)
        AdjustmentReason.TRANSFER -> context.getString(R.string.boxes_reason_transfer)
        AdjustmentReason.OTHER -> context.getString(R.string.boxes_reason_other)
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
