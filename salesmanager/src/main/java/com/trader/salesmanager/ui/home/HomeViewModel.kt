package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.domain.usecase.GetDashboardAnalyticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: TransactionRepository,
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository,
    getDashboardAnalytics: GetDashboardAnalyticsUseCase
) : ViewModel() {

    private val _merchantId = MutableStateFlow("")
    private val dashboardAnalytics = getDashboardAnalytics().distinctUntilChanged()

    init {
        viewModelScope.launch {
            _merchantId.value = activationRepo.getMerchantCode()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        repo.observeRecentTransactions(5),
        dashboardAnalytics,
        _merchantId.flatMapLatest { id ->
            if (id.isEmpty()) flowOf(0)
            else chatRepo.getUnreadCount(id, excludeSenderId = id)
        }
    ) { recentTransactions, analytics, unread ->
        HomeUiState(
            todayTotal = analytics.todaySummary.totalSales,
            todayPaid = analytics.todaySummary.paidSales,
            todayUnpaid = analytics.todaySummary.unpaidSales,
            recentTransactions = recentTransactions,
            unreadChatCount = unread,
            dashboard = DashboardUiState(
                todaySales = analytics.todaySummary.totalSales,
                todayInvoiceCount = analytics.todaySummary.invoiceCount,
                totalOutstandingDebt = analytics.totalOutstandingDebt,
                topSellingProducts = analytics.topSellingProducts,
                topDebtorCustomers = analytics.topDebtorCustomers,
                lastSevenDaysSales = analytics.lastSevenDaysSales
            ),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(isLoading = true))
}
