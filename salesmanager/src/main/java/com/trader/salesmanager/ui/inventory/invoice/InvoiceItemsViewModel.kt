package com.trader.salesmanager.ui.inventory.invoice

import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════
// InvoiceLineItem
//
// المفتاح الهندسي:
//   • displayQty       = ما يُدخله البائع (بوحدة displayWeightUnit)
//   • displayWeightUnit= الوحدة التجارية المختارة (كيلو / غرام / أوقية / رطل)
//   • quantity         = الكمية بالكيلو (تُخصم من المخزون) — محسوبة تلقائياً
//
// هكذا يُدخل البائع "2 رطل"، ويخصم النظام 0.9072 كيلو من المخزون.
// ═══════════════════════════════════════════════════════════════════

data class InvoiceLineItem(
    val tempId: String = UUID.randomUUID().toString(),
    val product: ProductWithUnits,
    val selectedUnit: ProductUnit,

    /** الكمية كما أدخلها البائع بـ displayWeightUnit */
    val displayQty: Double = 1.0,

    /**
     * وحدة عرض البائع.
     * الافتراضي = KG إذا كانت وحدة المنتج WEIGHT، وإلا KG أيضاً
     * (المنتجات غير الوزنية لا تستخدم التحويل).
     */
    val displayWeightUnit: SaleWeightUnit = SaleWeightUnit.KG,

    val customPrice: Double? = null
) {
    /**
     * الكمية بالكيلو — هذه هي القيمة التي تُخصم من المخزون وتُحفظ في InvoiceItem.
     * إذا كانت وحدة المنتج WEIGHT → تحويل حقيقي.
     * إذا كانت غير وزنية (PIECE/CARTON) → displayQty مباشرة (لا تحويل).
     */
    val quantity: Double get() = when {
        selectedUnit.unitType == UnitType.WEIGHT ->
        toKgQuantity(displayQty, displayWeightUnit)
        else -> displayQty
    }

    val effectivePrice: Double get() = customPrice ?: selectedUnit.price
    val totalPrice: Double     get() = effectivePrice * quantity

    /** نص مختصر للعرض في بطاقة الصنف */
    val displayQtyLabel: String get() = when {
        selectedUnit.unitType == UnitType.WEIGHT ->
        "${displayQty.formatQty()} ${displayWeightUnit.labelAr}"
        else ->
        displayQty.formatQty()
    }

    /** نص الكمية بالكيلو لعرضه في الفاتورة إلى جانب الوحدة الأصلية */
    val kgLabel: String get() = when {
        selectedUnit.unitType == UnitType.WEIGHT && displayWeightUnit != SaleWeightUnit.KG ->
        "(${quantity.formatQty()} كيلو)"
        else -> ""
    }
}

data class InvoiceItemsUiState(
    val allProducts: List<ProductWithUnits> = emptyList(),
    val searchQuery: String = "",
    val lines: List<InvoiceLineItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val searchResults: List<ProductWithUnits> get() = if (searchQuery.isEmpty()) emptyList()
    else allProducts.filter {
        it.product.name.contains(searchQuery, ignoreCase = true) ||
        it.product.barcode?.contains(searchQuery) == true ||
        it.product.category.contains(searchQuery, ignoreCase = true)
    }.take(10)

    val totalAmount: Double get() = lines.sumOf {
        it.totalPrice
    }
    val totalItems: Int    get() = lines.sumOf {
        it.displayQty.toInt().coerceAtLeast(1)
    }
}

class InvoiceItemsViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _lines = MutableStateFlow<List<InvoiceLineItem>>(emptyList())

    val uiState: StateFlow<InvoiceItemsUiState> = combine(
        productRepo.getAllProducts(), _query, _lines
    ) {
        products, query, lines ->
        InvoiceItemsUiState(allProducts = products, searchQuery = query, lines = lines, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceItemsUiState())

    fun setQuery(q: String) {
        _query.value = q
    }
    fun clearQuery() {
        _query.value = ""
    }

    fun onBarcodeScanned(barcode: String, onNotFound: (String) -> Unit) {
        viewModelScope.launch {
            val product = productRepo.getProductByBarcode(barcode)
            if (product != null) addProduct(product) else onNotFound(barcode)
        }
    }

    fun addProduct(product: ProductWithUnits) {
        val unit = product.defaultUnit ?: return
        val idx = _lines.value.indexOfFirst {
            it.product.product.id == product.product.id && it.selectedUnit.id == unit.id
        }
        if (idx >= 0) updateDisplayQty(idx, _lines.value[idx].displayQty + 1)
        else _lines.update {
            it + InvoiceLineItem(product = product, selectedUnit = unit)
        }
        _query.value = ""
    }

    fun addProductWithUnit(product: ProductWithUnits, unit: ProductUnit) {
        val idx = _lines.value.indexOfFirst {
            it.product.product.id == product.product.id && it.selectedUnit.id == unit.id
        }
        if (idx >= 0) updateDisplayQty(idx, _lines.value[idx].displayQty + 1)
        else _lines.update {
            it + InvoiceLineItem(product = product, selectedUnit = unit)
        }
        _query.value = ""
    }

    fun removeLine(index: Int) {
        _lines.update {
            it.toMutableList().also {
                l -> l.removeAt(index)
            }
        }
    }

    /**
     * تحديث الكمية المُعروضة (بوحدة البائع).
     * quantity (بالكيلو) تُحسب تلقائياً داخل InvoiceLineItem.
     */
    fun updateDisplayQty(index: Int, qty: Double) {
        if (qty <= 0) {
            removeLine(index); return
        }
        _lines.update {
            list ->
            list.mapIndexed {
                i, l -> if (i == index) l.copy(displayQty = qty) else l
            }
        }
    }

    /** للتوافق مع الكود القديم الذي يستدعي updateQuantity */
    fun updateQuantity(index: Int, qty: Double) = updateDisplayQty(index, qty)

    fun updatePrice(index: Int, price: Double?) {
        _lines.update {
            list ->
            list.mapIndexed {
                i, l -> if (i == index) l.copy(customPrice = price) else l
            }
        }
    }

    fun updateUnit(index: Int, unit: ProductUnit) {
        _lines.update {
            list ->
            list.mapIndexed {
                i, l ->
                if (i == index) l.copy(selectedUnit = unit, customPrice = null) else l
            }
        }
    }

    /** تحديث وحدة الوزن التجارية (كيلو / غرام / أوقية / رطل) */
    fun updateWeightUnit(index: Int, weightUnit: SaleWeightUnit) {
        _lines.update {
            list ->
            list.mapIndexed {
                i, l ->
                if (i == index) {
                    // نحوّل الكمية المعروضة لتبقى مكافئة للكيلو الحالي
                    val currentKg = l.quantity
                    val newDisplay = fromKgQuantity(currentKg, weightUnit)
                    l.copy(displayWeightUnit = weightUnit, displayQty = newDisplay)
                } else l
            }
        }
    }

    fun toInvoiceItems(transactionId: Long, merchantId: String): List<InvoiceItem> =
    _lines.value.map {
        line ->
        InvoiceItem(
            id = UUID.randomUUID().toString(),
            transactionId = transactionId,
            productId = line.product.product.id,
            productName = line.product.product.name,
            unitId = line.selectedUnit.id,
            // نحفظ الوحدة التجارية في unitLabel لعرض الفاتورة لاحقاً
            unitLabel = if (line.displayWeightUnit != SaleWeightUnit.KG && line.selectedUnit.unitType == UnitType.WEIGHT)
                "${line.selectedUnit.unitLabel} (${line.displayWeightUnit.labelAr})"
            else line.selectedUnit.unitLabel,
            quantity = line.quantity, // بالكيلو دائماً
            pricePerUnit = line.effectivePrice,
            totalPrice = line.totalPrice,
            merchantId = merchantId
        )
    }

    fun clearLines() {
        _lines.value = emptyList()
    }

    fun loadExistingLines(json: String) {
        viewModelScope.launch {
            try {
                val arr = org.json.JSONArray(json)
                val rebuilt = mutableListOf<InvoiceLineItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val productId = obj.getString("productId")
                    val unitId = obj.getString("unitId")
                    val price = obj.getDouble("price")
                    val displayQty = obj.getDouble("displayQty")
                    val weightUnit = runCatching {
                        SaleWeightUnit.valueOf(obj.optString("displayWeightUnit", "KG"))
                    }.getOrDefault(SaleWeightUnit.KG)
                    val productWithUnits = productRepo.getProductById(productId) ?: continue
                    val unit = productWithUnits.units.firstOrNull {
                        it.id == unitId
                    } ?: continue
                    rebuilt += InvoiceLineItem(
                        product = productWithUnits,
                        selectedUnit = unit,
                        displayQty = displayQty,
                        displayWeightUnit = weightUnit,
                        customPrice = if (price != unit.price) price else null
                    )
                }

                _lines.value = rebuilt

            } catch (_: Exception) {}
        }
    }
}