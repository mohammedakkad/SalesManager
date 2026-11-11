package com.trader.salesmanager.ui.inventory.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Product
import com.trader.core.domain.model.ProductUnit
import com.trader.core.domain.model.UnitType
import com.trader.core.domain.model.WeightUnit
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


/**
 * يُصفّي المدخل للسماح بالأرقام الموجبة فقط (عشرية).
 */
private fun String.filterPositiveDecimal(): String {
    val digits = filter { it.isDigit() || it == '.' }
    val dot = digits.indexOf('.')
    val normalized = if (dot >= 0) {
        digits.substring(0, dot + 1) + digits.substring(dot + 1).filter { it.isDigit() }
    } else digits
    val trimmed = normalized.trimStart('0')
    // A typed zero is distinct from clearing an optional field with a nonzero default.
    return trimmed.ifEmpty { if (normalized.isEmpty()) "" else "0" }
}

data class UnitDraft(
    val id: String = UUID.randomUUID().toString(),
    val unitType: UnitType = UnitType.PIECE,
    val unitLabel: String = "حبة",
    val price: String = "",
    val costPrice: String = "",
    val quantityInStock: String = "",
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
    val deletedUnitIds: Set<String> = emptySet(), // IDs الوحدات المحذوفة (موجودة في DB)
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) {
    val isValid: Boolean
    get() = name.isNotBlank()
    && units.isNotEmpty()
    && units.all {
        it.unitLabel.isNotBlank() &&
            it.price.toDoubleOrNull()?.let { price -> price.isFinite() && price > 0 } == true &&
            listOf(it.costPrice, it.quantityInStock, it.lowStockThreshold).all { raw ->
                raw.toDoubleOrNull()?.isFinite() != false
            }
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
    private var barcodeValidationRevision = 0L
    private var persistedUnitIds: Set<String> = emptySet()

    fun initWithBarcode(barcode: String) {
        _state.update {
            it.copy(barcode = barcode)
        }
        validateBarcode(barcode)
    }

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            val p = productRepo.getProductById(productId) ?: return@launch
            barcodeCheckJob?.cancel()
            barcodeValidationRevision += 1
            persistedUnitIds = p.units.map { it.id }.toSet()
            _state.update {
                state ->
                state.copy(
                    productId = p.product.id,
                    barcode = p.product.barcode ?: "",
                    name = p.product.name,
                    category = p.product.category,
                    isEditing = true,
                    barcodeChecking = false,
                    barcodeConflict = null,
                    units = p.units.map {
                        u ->
                        UnitDraft(
                            id = u.id,
                            unitType = u.unitType,
                            unitLabel = u.unitLabel,
                            price = u.price.toString(),
                            costPrice = if (u.costPrice > 0) u.costPrice.toString() else "",
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
        val revision = ++barcodeValidationRevision
        val productId = _state.value.productId
        fun isCurrent() = revision == barcodeValidationRevision &&
            _state.value.barcode == barcode && _state.value.productId == productId
        _state.update {
            it.copy(barcodeConflict = null, barcodeChecking = barcode.isNotBlank(), error = null)
        }
        if (barcode.isBlank()) return
        barcodeCheckJob = viewModelScope.launch {
            try {
                delay(350)
                val conflictName = productRepo.getBarcodeConflict(barcode, productId)
                if (isCurrent()) {
                    _state.update { state -> state.copy(
                        barcodeConflict = conflictName?.let { "مستخدم للصنف: $it" }
                    ) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (isCurrent()) _state.update { it.copy(error = "تعذر التحقق من الباركود، حاول مجددًا") }
            } finally {
                if (isCurrent()) _state.update { it.copy(barcodeChecking = false) }
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
            val removed = s.units[index]
            val list = s.units.toMutableList().also {
                it.removeAt(index)
            }

            // ✅ تعيين افتراضي تلقائي إذا حُذفت الوحدة الافتراضية
            val fixed = if (list.none {
                it.isDefault
            } && list.isNotEmpty())
            list.mapIndexed {
                i, u -> if (i == 0) u.copy(isDefault = true) else u
            } else list

            // Only persisted identities belong in the deletion request. New drafts also have UUIDs.
            val newDeleted = if (removed.id in persistedUnitIds)
                s.deletedUnitIds + removed.id
            else s.deletedUnitIds

            s.copy(units = fixed, deletedUnitIds = newDeleted)
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

    fun updateUnitCostPrice(index: Int, raw: String) {
        updateUnit(index, _state.value.units[index].copy(costPrice = raw.filterPositiveDecimal()))
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
        if (!s.isValid || s.isSaving || s.savedSuccessfully) return
        // Claim submission before launching so consecutive taps cannot enqueue two writes.
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
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
                        costPrice = (draft.costPrice.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        quantityInStock = (draft.quantityInStock.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        itemsPerCarton = draft.itemsPerCarton.toIntOrNull(),
                        lowStockThreshold = (draft.lowStockThreshold.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0),
                        isDefault = draft.isDefault,
                        weightUnit = draft.weightUnit
                    )
                }
                productRepo.saveProduct(product, units, s.deletedUnitIds)
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