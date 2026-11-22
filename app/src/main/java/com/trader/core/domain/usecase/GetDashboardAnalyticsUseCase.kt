package com.trader.core.domain.usecase

import com.trader.core.domain.model.DashboardAnalytics
import com.trader.core.domain.repository.DashboardAnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

class GetDashboardAnalyticsUseCase(
    private val repository: DashboardAnalyticsRepository
) {
    operator fun invoke(nowMillis: Long = System.currentTimeMillis()): Flow<DashboardAnalytics> {
        val todayStart = startOfDay(nowMillis)
        val todayEnd = endOfDay(nowMillis)
        val monthStart = startOfMonth(nowMillis)
        val monthEnd = endOfMonth(nowMillis)
        val dayBoundaries = lastSevenDayBoundaries(todayStart)

        return combine(
            repository.observeTodaySalesSummary(todayStart, todayEnd),
            repository.observeTotalOutstandingDebt(),
            repository.observeTopSellingProducts(monthStart, monthEnd, DASHBOARD_LIMIT),
            repository.observeTopDebtorCustomers(DASHBOARD_LIMIT),
            repository.observeLastSevenDaysSales(dayBoundaries)
        ) { todaySummary, totalDebt, topProducts, topDebtors, dailySales ->
            DashboardAnalytics(
                todaySummary = todaySummary,
                totalOutstandingDebt = totalDebt,
                topSellingProducts = topProducts,
                topDebtorCustomers = topDebtors,
                lastSevenDaysSales = dailySales
            )
        }.flowOn(Dispatchers.IO)
    }

    private fun startOfDay(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun endOfDay(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    private fun startOfMonth(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun endOfMonth(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            this.timeInMillis = startOfMonth(timeMillis)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis

    private fun lastSevenDayBoundaries(todayStart: Long): List<Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -6)
        }
        return buildList {
            repeat(8) {
                add(calendar.timeInMillis)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private companion object {
        const val DASHBOARD_LIMIT = 5
    }
}
