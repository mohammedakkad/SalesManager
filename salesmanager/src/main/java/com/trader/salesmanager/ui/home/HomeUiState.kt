package com.trader.salesmanager.ui.home

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayPaid: Double = 0.0,
    val todayUnpaid: Double = 0.0,
    val isLoading: Boolean = false
)