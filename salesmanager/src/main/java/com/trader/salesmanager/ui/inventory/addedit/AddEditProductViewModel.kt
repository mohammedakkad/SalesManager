package com.trader.salesmanager.ui.inventory.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


/**
 * يُصفّي المدخل للسماح بالأرقام الموجبة فقط (عشرية).
 * يُستخدم لحقول السعر والكمية — يمنع الإشارة السالبة من الجذر.
 */
private fun String.filterPositiveDecimal(): String {
    val filtered = this
    .filter {
        it.isDigit() || it == '.'
    } // أرقام ونقطة فقط — بدون '-'
    .let {
        s ->
        // نقطة واحدة فقط
        val dotIdx = s.indexOf('.')
        if (dotIdx >= 0) s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter {
            it.isDigit()
        } else s
    }
    .trimStart('0').let {
        if (it.startsWith('.') || it.isEmpty()) it else it
    }
    return filtered.ifEmpty {
        ""
    }
}

data class UnitDraft(
    val id: String = UUID.randomUUID().toString(),
    val unitType: UnitType = UnitType.PIECE,
    val unitLabel: String = "حبة",
    val price: String = "",
    val quantityInStock: String = "0",
    val itemsPerCarton: String = "",
    val lowStockThreshold: String = "5",
    val isDefault: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG // ← جديد
)

data class AddEditProductUiState(
    val productId: String? = null,
    val barcode: String = "",
    val name: String = "",
    val category: String = "",
    val units: List<UnitDraft> = listOf(UnitDraft(isDefault = true)),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank() && units.isNotEmpty() &&
    units.all {
        it.unitLabel.isNotBlank() && (it.price.toDoubleOrNull() ?: -1.0) > 0
    }
}

class AddEditProductViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _state.asStateFlow()

    fun initWithBarcode(barcode: String) {
        _state.update {
            it.copy(barcode = barcode)
        }
    }

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            val p = productRepo.getProductById(productId) ?: return@launch
            _state.update {
                state ->
                state.copy(
                    productId = p.product.id,
                    barcode = p.product.barcode ?: "",
                    name = p.product.name,
                    category = p.product.category,
                    isEditing = true,
                    units = p.units.map {
                        u ->
                        UnitDraft(
                            id = u.id,
                            unitType = u.unitType,
                            unitLabel = u.unitLabel,
                            price = u.price.toString(),
                            quantityInStock = u.quantityInStock.toString(),
                            itemsPerCarton = u.itemsPerCarton?.toString() ?: "",
                            lowStockThreshold = u.lowStockThreshold.toString(),
                            isDefault = u.isDefault,
                            weightUnit = u.weightUnit
                        )
                    }
                )
            }
        }
    }

    fun setBarcode(v: String) = _state.update {
        it.copy(barcode = v)
    }
    fun setName(v: String) = _state.update {
        it.copy(name = v)
    }
    fun setCategory(v: String) = _state.update {
        it.copy(category = v)
    }

    fun addUnit() {
        _state.update {
            s ->
            s.copy(units = s.units + UnitDraft())
        }
    }

    fun removeUnit(index: Int) {
        _state.update {
            s ->
            val list = s.units.toMutableList().also {
                it.removeAt(index)
            }
            // نضمن وجود وحدة افتراضية
            val fixed = if (list.none {
                it.isDefault
            } && list.isNotEmpty())
            list.mapIndexed {
                i, u -> if (i == 0) u.copy(isDefault = true) else u
            } else list
            s.copy(units = fixed)
        }
    }

    fun updateUnit(index: Int, updated: UnitDraft) {
        _state.update {
            s ->
            val list = s.units.toMutableList().also {
                it[index] = updated
            }
            s.copy(units = list)
        }
    }

    /** ✅ يُصفّي السعر لمنع القيم السالبة */
    fun updateUnitPrice(index: Int, raw: String) {
        val safe = raw.filterPositiveDecimal()
        updateUnit(index, _state.value.units[index].copy(price = safe))
    }

    /** ✅ يُصفّي الكمية لمنع القيم السالبة */
    fun updateUnitQty(index: Int, raw: String) {
        val safe = raw.filterPositiveDecimal()
        updateUnit(index, _state.value.units[index].copy(quantityInStock = safe))
    }

    /** ✅ يُصفّي حد التنبيه لمنع القيم السالبة */
    fun updateUnitLowStock(index: Int, raw: String) {
        val safe = raw.filterPositiveDecimal()
        updateUnit(index, _state.value.units[index].copy(lowStockThreshold = safe))
    }

    fun setDefaultUnit(index: Int) {
        _state.update {
            s ->
            s.copy(units = s.units.mapIndexed {
                i, u -> u.copy(isDefault = i == index)
            })
        }
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update {
                it.copy(isSaving = true, error = null)
            }
            try {
                val product = Product(
                    id = s.productId ?: "",
                    barcode = s.barcode.ifBlank {
                        null
                    },
                    name = s.name.trim(),
                    category = s.category.trim()
                )
                val units = s.units.map {
                    draft ->
                    ProductUnit(
                        id = draft.id,
                        unitType = draft.unitType,
                        unitLabel = draft.unitLabel.trim(),
                        price = (draft.price.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        quantityInStock = (draft.quantityInStock.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        itemsPerCarton = draft.itemsPerCarton.toIntOrNull(),
                        lowStockThreshold = (draft.lowStockThreshold.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0),
                        isDefault = draft.isDefault,
                        weightUnit = draft.weightUnit
                    )
                }
                productRepo.saveProduct(product, units)
                _state.update {
                    it.copy(isSaving = false, savedSuccessfully = true)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = "خطأ: ${e.message}")
                }
            }
        }
    }
}