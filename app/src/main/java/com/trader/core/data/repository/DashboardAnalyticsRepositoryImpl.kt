package com.trader.core.data.repository

import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.domain.model.DailySales
import com.trader.core.domain.model.TodaySalesSummary
import com.trader.core.domain.model.TopDebtorCustomer
import com.trader.core.domain.model.TopSellingProduct
import com.trader.core.domain.repository.DashboardAnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DashboardAnalyticsRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val invoiceItemDao: InvoiceItemDao
) : DashboardAnalyticsRepository {
    override fun observeTodaySalesSummary(
        startDate: Long,
        endDate: Long
    ): Flow<TodaySalesSummary> =
        transactionDao.observeTodaySalesSummary(startDate, endDate).map { row ->
            TodaySalesSummary(
                totalSales = row.todaySales,
                invoiceCount = row.todayInvoiceCount,
                paidSales = row.todayPaid,
                unpaidSales = row.todayUnpaid
            )
        }

    override fun observeTotalOutstandingDebt(): Flow<Double> =
        transactionDao.observeTotalOutstandingDebt()

    override fun observeTopSellingProducts(
        startDate: Long,
        endDate: Long,
        limit: Int
    ): Flow<List<TopSellingProduct>> =
        invoiceItemDao.observeTopSellingProducts(startDate, endDate, limit).map { rows ->
            rows.map { row ->
                TopSellingProduct(
                    productId = row.productId,
                    productName = row.productName,
                    quantitySold = row.quantitySold
                )
            }
        }

    override fun observeTopDebtorCustomers(limit: Int): Flow<List<TopDebtorCustomer>> =
        transactionDao.observeTopDebtorCustomers(limit).map { rows ->
            rows.map { row ->
                TopDebtorCustomer(
                    customerId = row.customerId,
                    customerName = row.customerName,
                    totalDebt = row.totalDebt
                )
            }
        }

    override fun observeLastSevenDaysSales(
        startDate: Long,
        endDate: Long
    ): Flow<List<DailySales>> =
        transactionDao.observeLastSevenDaysSales(startDate, endDate).map { rows ->
            rows.map { row ->
                DailySales(
                    dayStartMillis = row.dayStartMillis,
                    totalSales = row.totalSales
                )
            }
        }
}
