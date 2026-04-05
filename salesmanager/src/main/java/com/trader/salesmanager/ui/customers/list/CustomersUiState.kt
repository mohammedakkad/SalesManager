package com.trader.salesmanager.ui.customers.list

import com.trader.salesmanager.domain.model.Customer

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)