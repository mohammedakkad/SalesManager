package com.trader.salesmanager.ui.customers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class CustomersViewModel(private val repo: CustomerRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    // Starts true; flipped to false on the first Room emission so the screen
    // shows a spinner while the DB query runs instead of a premature empty-state.
    private val _isLoading = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _customers: Flow<List<Customer>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isEmpty()) repo.getAllCustomers() else repo.searchCustomers(q)
        }
        .onEach { _isLoading.value = false }

    val uiState: StateFlow<CustomersUiState> = combine(_customers, _searchQuery, _isLoading) {
        customers, query, loading ->
        val pendingCount = customers.count { it.syncStatus == SyncStatus.PENDING }
        CustomersUiState(
            customers = customers,
            searchQuery = query,
            isLoading = loading,
            pendingSyncCount = pendingCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomersUiState(isLoading = true))

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    // ✅ مشكلة 3: فحص العمليات المرتبطة قبل الحذف
    fun requestDelete(customer: Customer) {
        viewModelScope.launch {
            val count = repo.getTransactionCount(customer.id)
            uiState.value.let {}
            // نُحدِّث الـ state عبر منفصل لأن uiState مبني من combine
            _deleteConfirm.value = DeleteConfirmState(customer, count)
        }
    }

    private val _deleteConfirm = MutableStateFlow<DeleteConfirmState?>(null)
    val deleteConfirm: StateFlow<DeleteConfirmState?> = _deleteConfirm.asStateFlow()

    fun confirmDelete() {
        val c = _deleteConfirm.value?.customer ?: return
        _deleteConfirm.value = null
        viewModelScope.launch {
            repo.deleteCustomer(c)
        }
    }

    fun dismissDelete() {
        _deleteConfirm.value = null
    }
}