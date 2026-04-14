package com.trader.salesmanager.ui.inventory.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.InventoryRepository
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventorySessionUiState(
    val activeSession: InventorySession? = null,
    val pastSessions: List<InventorySession> = emptyList(),
    val isLoading: Boolean = true,
    val isFinishing: Boolean = false,
    val searchQuery: String = "",
    val showOnlyPending: Boolean = false
)

class InventorySessionViewModel(
    private val inventoryRepo: InventoryRepository,
    private val productRepo: ProductRepository,
    private val merchantId: String
) : ViewModel() {

    private val _search      = MutableStateFlow("")
    private val _pendingOnly = MutableStateFlow(false)
    private val _isFinishing = MutableStateFlow(false)

    val uiState: StateFlow<InventorySessionUiState> = combine(
        inventoryRepo.getActiveSession(),
        inventoryRepo.getAllSessions(),
        _search, _pendingOnly, _isFinishing
    ) { active, all, query, pendingOnly, finishing ->
        InventorySessionUiState(
            activeSession  = active,
            pastSessions   = all.filter { it.status == InventoryStatus.FINISHED }.take(5),
            isLoading      = false,
            isFinishing    = finishing,
            searchQuery    = query,
            showOnlyPending = pendingOnly
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventorySessionUiState())

    val sessionItems: StateFlow<List<InventorySessionItem>> =
        inventoryRepo.getActiveSession().flatMapLatest { session ->
            if (session != null) inventoryRepo.getSessionItems(session.id)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val countedItems: Int    get() = sessionItems.value.count { it.actualQuantity != null }
    val totalItems: Int      get() = sessionItems.value.size
    val progressPercent: Float get() = if (totalItems == 0) 0f else countedItems.toFloat() / totalItems
    val adjustmentsCount: Int get() = sessionItems.value.count {
        it.actualQuantity != null && kotlin.math.abs(it.difference) > 0.001
    }

    fun setSearch(q: String)   { _search.value = q }
    fun togglePendingOnly()    { _pendingOnly.update { !it } }

    fun startNewSession() {
        viewModelScope.launch {
            val sessionId = inventoryRepo.startNewSession(merchantId)
            (inventoryRepo as? com.trader.core.data.repository.InventoryRepositoryImpl)
                ?.populateSessionItems(sessionId)
        }
    }

    fun updateItemCount(item: InventorySessionItem, actualQty: Double) {
        viewModelScope.launch { inventoryRepo.updateSessionItem(item.copy(actualQuantity = actualQty)) }
    }

    fun clearItemCount(item: InventorySessionItem) {
        viewModelScope.launch { inventoryRepo.updateSessionItem(item.copy(actualQuantity = null)) }
    }

    fun finishSession() {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch {
            _isFinishing.value = true
            inventoryRepo.finishSession(session.id)
            _isFinishing.value = false
        }
    }

    fun cancelSession() {
        val session = uiState.value.activeSession ?: return
        viewModelScope.launch { inventoryRepo.cancelSession(session.id) }
    }
}
