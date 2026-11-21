package com.trader.salesmanager.ui.home

import com.trader.core.domain.model.DailySales
import com.trader.core.domain.model.TopDebtorCustomer
import com.trader.core.domain.model.TopSellingProduct
import com.trader.core.domain.model.Transaction

data class DashboardUiState(
    val todaySales: Double = 0.0,
    val todayInvoiceCount: Int = 0,
    val totalOutstandingDebt: Double = 0.0,
    val topSellingProducts: List<TopSellingProduct> = emptyList(),
    val topDebtorCustomers: List<TopDebtorCustomer> = emptyList(),
    val lastSevenDaysSales: List<DailySales> = emptyList()
)

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayPaid: Double = 0.0,
    val todayUnpaid: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val unreadChatCount: Int = 0,
    val dashboard: DashboardUiState = DashboardUiState(),
    val isLoading: Boolean = false
)
