package com.trader.salesmanager.ui.inventory.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class InvoiceLineItem(
    val tempId: String = UUID.randomUUID().toString(),
    val product: ProductWithUnits,
    val selectedUnit: ProductUnit,
    val quantity: Double = 1.0,
    val customPrice: Double? = null
) {
    val effectivePrice: Double get() = customPrice ?: selectedUnit.price
    val totalPrice: Double get() = effectivePrice * quantity
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

    val totalAmount: Double get() = lines.sumOf { it.totalPrice }
    val totalItems: Int     get() = lines.sumOf { it.quantity.toInt() }
}

class InvoiceItemsViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _lines = MutableStateFlow<List<InvoiceLineItem>>(emptyList())

    val uiState: StateFlow<InvoiceItemsUiState> = combine(
        productRepo.getAllProducts(), _query, _lines
    ) { products, query, lines ->
        InvoiceItemsUiState(allProducts = products, searchQuery = query, lines = lines, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceItemsUiState())

    fun setQuery(q: String) { _query.value = q }
    fun clearQuery()        { _query.value = "" }

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
        if (idx >= 0) updateQuantity(idx, _lines.value[idx].quantity + 1)
        else _lines.update { it + InvoiceLineItem(product = product, selectedUnit = unit) }
        _query.value = ""
    }

    fun addProductWithUnit(product: ProductWithUnits, unit: ProductUnit) {
        val idx = _lines.value.indexOfFirst {
            it.product.product.id == product.product.id && it.selectedUnit.id == unit.id
        }
        if (idx >= 0) updateQuantity(idx, _lines.value[idx].quantity + 1)
        else _lines.update { it + InvoiceLineItem(product = product, selectedUnit = unit) }
        _query.value = ""
    }

    fun removeLine(index: Int) {
        _lines.update { it.toMutableList().also { l -> l.removeAt(index) } }
    }

    fun updateQuantity(index: Int, qty: Double) {
        if (qty <= 0) { removeLine(index); return }
        _lines.update { list -> list.mapIndexed { i, l -> if (i == index) l.copy(quantity = qty) else l } }
    }

    fun updatePrice(index: Int, price: Double?) {
        _lines.update { list -> list.mapIndexed { i, l -> if (i == index) l.copy(customPrice = price) else l } }
    }

    fun updateUnit(index: Int, unit: ProductUnit) {
        _lines.update { list -> list.mapIndexed { i, l -> if (i == index) l.copy(selectedUnit = unit, customPrice = null) else l } }
    }

    fun toInvoiceItems(transactionId: Long, merchantId: String): List<InvoiceItem> =
        _lines.value.map { line ->
            InvoiceItem(
                id = UUID.randomUUID().toString(),
                transactionId = transactionId,
                productId = line.product.product.id,
                productName = line.product.product.name,
                unitId = line.selectedUnit.id,
                unitLabel = line.selectedUnit.unitLabel,
                quantity = line.quantity,
                pricePerUnit = line.effectivePrice,
                totalPrice = line.totalPrice,
                merchantId = merchantId
            )
        }

    fun clearLines() { _lines.value = emptyList() }
}
