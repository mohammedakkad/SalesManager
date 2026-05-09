package com.trader.core.data.repository

import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.FinancialReportTotals
import com.trader.core.domain.model.InventoryValue
import com.trader.core.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReportsRepositoryImpl(
    private val invoiceItemDao: InvoiceItemDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) : ReportsRepository {

    override fun observeFinancialReportTotals(
        startDate: Long,
        endDate: Long
    ): Flow<FinancialReportTotals> =
        invoiceItemDao.observeFinancialReportTotals(startDate, endDate).map { projection ->
            FinancialReportTotals(
                totalSales = projection.totalSales,
                netProfit = projection.netProfit
            )
        }

    override fun observeDailySalesProfit(
        startDate: Long,
        endDate: Long,
        timezoneOffsetMillis: Long
    ): Flow<List<DailySalesProfit>> =
        invoiceItemDao.observeDailySalesProfit(startDate, endDate, timezoneOffsetMillis).map { rows ->
            rows.map { row ->
                DailySalesProfit(
                    dayStartMillis = row.dayStartMillis,
                    sales = row.sales,
                    profit = row.profit
                )
            }
        }

    override fun observeInventoryValue(): Flow<InventoryValue> =
        productDao.observeInventoryValue().map { projection ->
            InventoryValue(
                costValue = projection.costValue,
                saleValue = projection.saleValue
            )
        }

    override fun observeDebtAging(
        oneWeekAgo: Long,
        oneMonthAgo: Long,
        threeMonthsAgo: Long
    ): Flow<DebtAging> =
        transactionDao.observeDebtAging(oneWeekAgo, oneMonthAgo, threeMonthsAgo).map { projection ->
            DebtAging(
                lessThanWeekAmount = projection.lessThanWeekAmount,
                oneWeekToOneMonthAmount = projection.oneWeekToOneMonthAmount,
                oneMonthToThreeMonthsAmount = projection.oneMonthToThreeMonthsAmount,
                moreThanThreeMonthsAmount = projection.moreThanThreeMonthsAmount,
                lessThanWeekCount = projection.lessThanWeekCount,
                oneWeekToOneMonthCount = projection.oneWeekToOneMonthCount,
                oneMonthToThreeMonthsCount = projection.oneMonthToThreeMonthsCount,
                moreThanThreeMonthsCount = projection.moreThanThreeMonthsCount
            )
        }
}
