package com.trader.salesmanager.ui.home

import com.trader.core.domain.model.Transaction

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayPaid: Double = 0.0,
    val todayUnpaid: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),  // ← آخر 5 عمليات
    val unreadChatCount: Int = 0,                             // ← عدد الرسائل الجديدة
    val isLoading: Boolean = false
)
