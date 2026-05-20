package com.trader.salesmanager.ui.inventory.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryListUiState(
    val products: List<ProductWithUnits> = emptyList(),
    val query: String = "",
    val filter: StockFilter = StockFilter.ALL,
    val isLoading: Boolean = true,
    val showScanner: Boolean = false,
    // ✅ مؤشر حالة الشبكة — يُعرض للمستخدم
    val isOnline: Boolean = true,
    // ✅ عدد الأصناف التي لم تُزامن بعد (PENDING)
    val pendingSyncCount: Int = 0
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

    val totalProducts: Int  get() = products.size
    val lowStockCount: Int  get() = products.count {
        it.isLowStock
    }
    val outOfStockCount: Int get() = products.count {
        it.isOutOfStock
    }

    // ✅ منتج معلّق = له وحدات (موجود محلياً كاملاً) لكن لم يُزامن مع Remote بعد
    val hasPendingSync: Boolean get() = pendingSyncCount > 0
}

enum class StockFilter {
    ALL, LOW, OUT
}

class InventoryListViewModel(
    private val productRepo: ProductRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(StockFilter.ALL)

    // Incrementing this triggers flatMapLatest to restart the inner combine,
    // guaranteeing Room re-queries and the UI sees the latest data after navigation.
    private val _refreshVersion = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InventoryListUiState> =
        // Outer flatMapLatest means each refreshOnResume() call restarts the inner combine
        // entirely, forcing Room to re-query and guaranteeing fresh state after navigation.
        _refreshVersion.flatMapLatest {
            combine(
                productRepo.getAllProducts(),
                _query,
                _filter,
                networkMonitor.isOnlineFlow
            ) { products, query, filter, isOnline ->

                val pendingCount = products.count { p ->
                    p.units.isNotEmpty() && p.units.any { it.syncStatus == SyncStatus.PENDING }
                }

                InventoryListUiState(
                    products = products.filter { it.units.isNotEmpty() },
                    query = query,
                    filter = filter,
                    isLoading = false,
                    isOnline = isOnline,
                    pendingSyncCount = pendingCount
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InventoryListUiState()
        )

    /** Call when the screen enters RESUMED to guarantee fresh data is visible after navigation. */
    fun refreshOnResume() {
        _refreshVersion.value++
    }

    fun setQuery(q: String) {
        _query.value = q
    }
    fun setFilter(f: StockFilter) {
        _filter.value = f
    }

    fun onBarcodeScanned(barcode: String, onFound: (String) -> Unit, onNotFound: () -> Unit) {
        viewModelScope.launch {
            val product = productRepo.getProductByBarcode(barcode)
            if (product != null) onFound(product.product.id)
            else onNotFound()
        }
    }
}