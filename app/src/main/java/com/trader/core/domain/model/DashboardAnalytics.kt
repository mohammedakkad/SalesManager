package com.trader.core.domain.model

data class TodaySalesSummary(
    val totalSales: Double = 0.0,
    val invoiceCount: Int = 0,
    val paidSales: Double = 0.0,
    val unpaidSales: Double = 0.0
)

data class TopSellingProduct(
    val productId: String,
    val productName: String,
    val quantitySold: Double
)

data class TopDebtorCustomer(
    val customerId: Long,
    val customerName: String,
    val totalDebt: Double
)

data class DailySales(
    val dayStartMillis: Long,
    val totalSales: Double
)

data class DashboardAnalytics(
    val todaySummary: TodaySalesSummary = TodaySalesSummary(),
    val totalOutstandingDebt: Double = 0.0,
    val topSellingProducts: List<TopSellingProduct> = emptyList(),
    val topDebtorCustomers: List<TopDebtorCustomer> = emptyList(),
    val lastSevenDaysSales: List<DailySales> = emptyList()
)
