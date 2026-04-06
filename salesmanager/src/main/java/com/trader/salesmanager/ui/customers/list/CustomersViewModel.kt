package com.trader.salesmanager.ui.customers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CustomersViewModel(private val repo: CustomerRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading   = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _customers: Flow<List<Customer>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q -> if (q.isEmpty()) repo.getAllCustomers() else repo.searchCustomers(q) }

    val uiState: StateFlow<CustomersUiState> = combine(_customers, _searchQuery, _isLoading) { customers, query, loading ->
        CustomersUiState(customers = customers, searchQuery = query, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomersUiState())

    fun updateSearch(query: String) { _searchQuery.value = query }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch { repo.deleteCustomer(customer) }
    }
}