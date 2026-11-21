package com.trader.core.domain.model

data class FinancialReportTotals(
    val totalSales: Double = 0.0,
    val netProfit: Double = 0.0
)

data class DailySalesProfit(
    val dayStartMillis: Long,
    val sales: Double,
    val profit: Double
)

data class InventoryValue(
    val costValue: Double = 0.0,
    val saleValue: Double = 0.0
)

data class DebtAging(
    val lessThanWeekAmount: Double = 0.0,
    val oneWeekToOneMonthAmount: Double = 0.0,
    val oneMonthToThreeMonthsAmount: Double = 0.0,
    val moreThanThreeMonthsAmount: Double = 0.0,
    val lessThanWeekCount: Int = 0,
    val oneWeekToOneMonthCount: Int = 0,
    val oneMonthToThreeMonthsCount: Int = 0,
    val moreThanThreeMonthsCount: Int = 0
) {
    val totalAmount: Double
        get() = lessThanWeekAmount +
            oneWeekToOneMonthAmount +
            oneMonthToThreeMonthsAmount +
            moreThanThreeMonthsAmount

    val totalCount: Int
        get() = lessThanWeekCount +
            oneWeekToOneMonthCount +
            oneMonthToThreeMonthsCount +
            moreThanThreeMonthsCount
}
