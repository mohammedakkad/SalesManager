package com.trader.salesmanager.ui.customers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.trader.core.domain.model.SyncStatus


class CustomersViewModel(private val repo: CustomerRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _customers: Flow<List<Customer>> = _searchQuery
    .debounce(300)
    .flatMapLatest {
        q -> if (q.isEmpty()) repo.getAllCustomers() else repo.searchCustomers(q)
    }

    val uiState: StateFlow<CustomersUiState> = combine(_customers, _searchQuery, _isLoading) {
        customers, query, loading ->
        // ✅ badge المزامنة: عدد العملاء الذين لم يُرفعوا بعد
        val pendingCount = customers.count {
            it.syncStatus == SyncStatus.PENDING
        }
        CustomersUiState(
            customers = customers,
            searchQuery = query,
            isLoading = loading,
            pendingSyncCount = pendingCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomersUiState())

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