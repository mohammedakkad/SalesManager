package com.trader.salesmanager.ui.inventory.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.MovementType
import com.trader.core.domain.model.ProductUnit
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.StockMovement
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class ManualAdjustState(
    val show: Boolean = false,
    val unitId: String = "",
    val unitLabel: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: String = "",
    val isAdd: Boolean = true,
    val note: String = "",
    val error: String? = null
)

data class ProductDetailUiState(
    val product: ProductWithUnits? = null,
    val movements: Map<String, List<StockMovement>> = emptyMap(), // unitId -> movements
    val isLoading: Boolean = true,
    val adjustState: ManualAdjustState = ManualAdjustState(),
    val isSaving: Boolean = false
)

class ProductDetailViewModel(
    private val productRepo: ProductRepository,
    private val stockRepo: StockRepository,
    private val productId: String
) : ViewModel() {

    private val _adjust = MutableStateFlow(ManualAdjustState())
    private val _isSaving = MutableStateFlow(false)

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productRepo.getAllProducts().map { list -> list.firstOrNull { it.product.id == productId } },
        _adjust,
        _isSaving
    ) { product, adjust, saving ->
        ProductDetailUiState(
            product = product,
            isLoading = product == null,
            adjustState = adjust,
            isSaving = saving
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    fun getMovementsForUnit(unitId: String): Flow<List<StockMovement>> =
        stockRepo.getMovementsForProduct(productId, unitId)

    fun showAdjust(unit: ProductUnit, isAdd: Boolean) {
        _adjust.value = ManualAdjustState(
            show = true,
            unitId = unit.id,
            unitLabel = unit.unitLabel,
            productId = productId,
            productName = uiState.value.product?.product?.name ?: "",
            isAdd = isAdd
        )
    }

    fun dismissAdjust() { _adjust.value = ManualAdjustState() }

    fun setAdjustQty(v: String)  { _adjust.update { it.copy(quantity = v, error = null) } }
    fun setAdjustNote(v: String) { _adjust.update { it.copy(note = v, error = null) } }

    fun applyAdjust() {
        val a = _adjust.value
        val qty = a.quantity.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return
        if (_isSaving.value) return
        // Reserve submission before dispatch so two taps cannot enqueue two writes.
        _isSaving.value = true
        _adjust.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                stockRepo.manualAdjust(
                    productId = a.productId,
                    unitId = a.unitId,
                    quantity = if (a.isAdd) qty else -qty,
                    type = if (a.isAdd) MovementType.MANUAL_IN else MovementType.MANUAL_OUT,
                    productName = a.productName,
                    unitLabel = a.unitLabel,
                    note = a.note
                )
                _adjust.value = ManualAdjustState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _adjust.update { it.copy(error = "تعذر تعديل المخزون. حاول مرة أخرى.") }
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteProduct(onDeleted: () -> Unit) {
        viewModelScope.launch {
            productRepo.deleteProduct(productId)
            onDeleted()
        }
    }
}
