package com.trader.salesmanager.ui.customers.list

import com.trader.core.domain.model.Customer

data class DeleteConfirmState(
    val customer: Customer,
    val transactionCount: Int
)

data class CustomersUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    // ✅ Default to true so first composition renders the loader.
    val isLoading: Boolean = true,
    val pendingSyncCount: Int = 0, // ✅ badge المزامنة
    val deleteConfirm: DeleteConfirmState? = null // ✅ dialog حذف ذكي
)