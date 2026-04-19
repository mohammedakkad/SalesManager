package com.trader.salesmanager.ui.inventory.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryListUiState(
    val products: List<ProductWithUnits> = emptyList(),
    val query: String = "",
    val filter: StockFilter = StockFilter.ALL,
    val isLoading: Boolean = true,
    val showScanner: Boolean = false
) {
    val filtered: List<ProductWithUnits> get() = products.filter {
        p ->
        val matchQuery = query.isEmpty() ||
        p.product.name.contains(query, ignoreCase = true) ||
        p.product.barcode?.contains(query) == true ||
        p.product.category.contains(query, ignoreCase = true)
        val matchFilter = when (filter) {
            StockFilter.ALL -> true
            StockFilter.LOW -> p.isLowStock
            StockFilter.OUT -> p.isOutOfStock
        }
        matchQuery && matchFilter
    }
    val totalProducts: Int get() = products.size
    val lowStockCount: Int get() = products.count {
        it.isLowStock
    }
    val outOfStockCount: Int get() = products.count {
        it.isOutOfStock
    }
}

enum class StockFilter {
    ALL, LOW, OUT
}

class InventoryListViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(StockFilter.ALL)

    val uiState: StateFlow<InventoryListUiState> = combine(
        productRepo.getAllProducts(),
        _query,
        _filter
    ) {
        products, query, filter ->
        InventoryListUiState(
            products = products,
            query = query,
            filter = filter,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly, // ✅ يبدأ فوراً ولا يتوقف أبداً
        InventoryListUiState()
    )

    fun setQuery(q: String) {
        _query.value = q
    }
    fun setFilter(f: StockFilter) {
        _filter.value = f
    }
    fun showScanner(show: Boolean) {
        /* handled by UI state in screen */
    }

    fun onBarcodeScanned(barcode: String, onFound: (String) -> Unit, onNotFound: () -> Unit) {
        viewModelScope.launch {
            val product = productRepo.getProductByBarcode(barcode)
            if (product != null) onFound(product.product.id)
            else onNotFound()
        }
    }
}