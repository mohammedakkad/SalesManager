package com.trader.salesmanager.ui.inventory.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.repository.StockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ManualAdjustState(
    val show: Boolean = false,
    val unitId: String = "",
    val unitLabel: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: String = "",
    val isAdd: Boolean = true,
    val note: String = ""
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

    fun setAdjustQty(v: String)  { _adjust.update { it.copy(quantity = v) } }
    fun setAdjustNote(v: String) { _adjust.update { it.copy(note = v) } }

    fun applyAdjust() {
        val a = _adjust.value
        val qty = a.quantity.toDoubleOrNull() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            stockRepo.manualAdjust(
                productId = a.productId,
                unitId = a.unitId,
                quantity = if (a.isAdd) qty else -qty,
                type = if (a.isAdd) MovementType.MANUAL_IN else MovementType.MANUAL_OUT,
                productName = a.productName,
                unitLabel = a.unitLabel,
                note = a.note
            )
            _isSaving.value = false
            _adjust.value = ManualAdjustState()
        }
    }

    fun deleteProduct(onDeleted: () -> Unit) {
        viewModelScope.launch {
            productRepo.deleteProduct(productId)
            onDeleted()
        }
    }
}
