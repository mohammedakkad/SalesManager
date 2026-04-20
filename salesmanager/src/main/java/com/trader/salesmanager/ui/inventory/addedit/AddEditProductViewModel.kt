package com.trader.salesmanager.ui.inventory.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


/**
 * يُصفّي المدخل للسماح بالأرقام الموجبة فقط (عشرية).
 */
private fun String.filterPositiveDecimal(): String {
    val filtered = this
    .filter {
        it.isDigit() || it == '.'
    }
    .let {
        s ->
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
    val weightUnit: WeightUnit = WeightUnit.KG
)

data class AddEditProductUiState(
    val productId: String? = null,
    val barcode: String = "",
    val barcodeConflict: String? = null, // اسم الصنف المتعارض، أو null
    val barcodeChecking: Boolean = false, // جارٍ التحقق...
    val name: String = "",
    val category: String = "",
    val units: List<UnitDraft> = listOf(UnitDraft(isDefault = true)),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) {
    val isValid: Boolean
    get() = name.isNotBlank()
    && units.isNotEmpty()
    && units.all {
        it.unitLabel.isNotBlank() && (it.price.toDoubleOrNull() ?: -1.0) > 0
    }
    && barcodeConflict == null
    && !barcodeChecking
}

class AddEditProductViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _state.asStateFlow()

    /** Job لإلغاء التحقق السابق عند كل ضغطة جديدة (debounce يدوي) */
    private var barcodeCheckJob: Job? = null

    fun initWithBarcode(barcode: String) {
        _state.update {
            it.copy(barcode = barcode)
        }
        validateBarcode(barcode)
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
            // عند التعديل لا نُظهر تعارضاً مع الباركود الحالي للصنف نفسه
            // لكن إذا تغيّر الباركود لاحقاً سيُتحقَّق منه
        }
    }

    fun setBarcode(v: String) {
        _state.update {
            it.copy(barcode = v, barcodeConflict = null)
        }
        validateBarcode(v)
    }

    fun setName(v: String) = _state.update {
        it.copy(name = v)
    }
    fun setCategory(v: String) = _state.update {
        it.copy(category = v)
    }

    // ── validation الباركود مع debounce ───────────────────────────

    private fun validateBarcode(barcode: String) {
        barcodeCheckJob?.cancel()
        if (barcode.isBlank()) {
            _state.update {
                it.copy(barcodeConflict = null, barcodeChecking = false)
            }
            return
        }
        barcodeCheckJob = viewModelScope.launch {
            _state.update {
                it.copy(barcodeChecking = true)
            }
            delay(350) // debounce — انتظر توقف الكتابة
            val conflictName = productRepo.getBarcodeConflict(barcode, _state.value.productId)
            _state.update {
                it.copy(
                    barcodeChecking = false,
                    barcodeConflict = conflictName?.let {
                        name -> "مستخدم للصنف: $name"
                    }
                )
            }
        }
    }

    // ── وحدات ─────────────────────────────────────────────────────

    fun addUnit() = _state.update {
        s -> s.copy(units = s.units + UnitDraft())
    }

    fun removeUnit(index: Int) {
        _state.update {
            s ->
            val list = s.units.toMutableList().also {
                it.removeAt(index)
            }
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
            s.copy(units = s.units.toMutableList().also {
                it[index] = updated
            })
        }
    }

    fun updateUnitPrice(index: Int, raw: String) {
        updateUnit(index, _state.value.units[index].copy(price = raw.filterPositiveDecimal()))
    }

    fun updateUnitQty(index: Int, raw: String) {
        updateUnit(index, _state.value.units[index].copy(quantityInStock = raw.filterPositiveDecimal()))
    }

    fun updateUnitLowStock(index: Int, raw: String) {
        updateUnit(index, _state.value.units[index].copy(lowStockThreshold = raw.filterPositiveDecimal()))
    }

    fun setDefaultUnit(index: Int) {
        _state.update {
            s ->
            s.copy(units = s.units.mapIndexed {
                i, u -> u.copy(isDefault = i == index)
            })
        }
    }

    // ── حفظ ───────────────────────────────────────────────────────

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            _state.update {
                it.copy(isSaving = true, error = null)
            }
            try {
                // ✅ حماية أخيرة: إعادة التحقق قبل الحفظ مباشرة (يمنع race condition)
                val barcode = s.barcode.ifBlank {
                    null
                }
                if (barcode != null) {
                    val conflictName = productRepo.getBarcodeConflict(barcode, s.productId)
                    if (conflictName != null) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                barcodeConflict = "مستخدم للصنف: $conflictName"
                            )
                        }
                        return@launch
                    }
                }

                val product = Product(
                    id = s.productId ?: "",
                    barcode = barcode,
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