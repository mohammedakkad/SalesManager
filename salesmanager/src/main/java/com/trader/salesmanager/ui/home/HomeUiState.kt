package com.trader.salesmanager.ui.home

import com.trader.core.domain.model.Transaction

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayPaid: Double = 0.0,
    val todayUnpaid: Double = 0.0,
    val yesterdayTotal: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val unreadChatCount: Int = 0,
    val isLoading: Boolean = false
) {
    val percentVsYesterday: Double
        get() = if (yesterdayTotal == 0.0) 0.0
                else ((todayTotal - yesterdayTotal) / yesterdayTotal) * 100.0
}
