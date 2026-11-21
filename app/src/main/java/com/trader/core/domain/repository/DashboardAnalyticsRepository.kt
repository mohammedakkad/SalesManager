package com.trader.core.domain.repository

import com.trader.core.domain.model.DailySales
import com.trader.core.domain.model.TodaySalesSummary
import com.trader.core.domain.model.TopDebtorCustomer
import com.trader.core.domain.model.TopSellingProduct
import kotlinx.coroutines.flow.Flow

interface DashboardAnalyticsRepository {
    fun observeTodaySalesSummary(startDate: Long, endDate: Long): Flow<TodaySalesSummary>

    fun observeTotalOutstandingDebt(): Flow<Double>

    fun observeTopSellingProducts(
        startDate: Long,
        endDate: Long,
        limit: Int
    ): Flow<List<TopSellingProduct>>

    fun observeTopDebtorCustomers(limit: Int): Flow<List<TopDebtorCustomer>>

    fun observeLastSevenDaysSales(dayBoundaries: List<Long>): Flow<List<DailySales>>
}
