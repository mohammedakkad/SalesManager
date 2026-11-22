package com.trader.core.domain.repository

import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.FinancialReportTotals
import com.trader.core.domain.model.InventoryValue
import kotlinx.coroutines.flow.Flow

interface ReportsRepository {
    fun observeFinancialReportTotals(
        startDate: Long,
        endDate: Long
    ): Flow<FinancialReportTotals>

    fun observeDailySalesProfit(
        startDate: Long,
        endDate: Long,
        timezoneOffsetMillis: Long
    ): Flow<List<DailySalesProfit>>

    fun observeInventoryValue(): Flow<InventoryValue>

    fun observeDebtAging(
        oneWeekAgo: Long,
        oneMonthAgo: Long,
        threeMonthsAgo: Long
    ): Flow<DebtAging>
}
