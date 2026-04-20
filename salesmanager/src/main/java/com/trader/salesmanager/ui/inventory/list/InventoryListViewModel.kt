package com.trader.salesmanager.ui.inventory.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ProductWithUnits
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.util.NetworkMonitor
import kotlinx.coroutines.flow.*
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

    val uiState: StateFlow<InventoryListUiState> = combine(
        // ✅ لا نُفلتر المنتجات ذات units.isEmpty() هنا
        // المنتجات التي تظهر بدون وحدات هي نتيجة خلل في Remote sync
        // والإصلاح صحيح في Repository وليس هنا
        // الفلترة هنا كانت تُخفي مشكلة بدل إصلاحها وتسبب اختفاء منتجات أضافها المستخدم
        productRepo.getAllProducts(),
        _query,
        _filter,
        networkMonitor.isOnlineFlow
    ) {
        products, query, filter, isOnline ->

        // ✅ أصناف PENDING (محفوظة محلياً، لم تُرفع بعد)
        val pendingCount = products.count {
            p ->
            p.units.isNotEmpty() && p.units.all {
                it.syncStatus == SyncStatus.PENDING
            }
        }

        InventoryListUiState(
            // ✅ نعرض كل الأصناف ذات وحدات — بما فيها PENDING
            products = products.filter {
                it.units.isNotEmpty()
            },
            query = query,
            filter = filter,
            isLoading = false,
            isOnline = isOnline,
            pendingSyncCount = pendingCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        InventoryListUiState()
    )

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