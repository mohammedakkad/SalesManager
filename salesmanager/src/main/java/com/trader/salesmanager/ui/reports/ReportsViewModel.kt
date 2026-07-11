package com.trader.salesmanager.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.FinancialReportTotals
import com.trader.core.domain.model.InventoryValue
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.ReportsRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ReportPeriod {
    TODAY, WEEK, MONTH
}

data class DaySalesEntry(val label: String, val total: Double, val paid: Double)
data class CustomerRank(val name: String, val amount: Double)
data class PaymentShare(val name: String, val amount: Double)
data class SalesProfitDayEntry(val label: String, val sales: Double, val profit: Double)

data class TimeOfDayAnalysis(
    val morningTotal: Double = 0.0,
    val afternoonTotal: Double = 0.0,
    val eveningTotal: Double = 0.0
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val txCount: Int = 0,
    val filteredTransactions: List<Transaction> = emptyList(),
    val dailySales: List<DaySalesEntry> = emptyList(),
    val paymentShares: List<PaymentShare> = emptyList(),
    val topSpenders: List<CustomerRank> = emptyList(),
    val topDebtors: List<CustomerRank> = emptyList(),
    val netProfit: Double = 0.0,
    val inventoryCostValue: Double = 0.0,
    val inventorySaleValue: Double = 0.0,
    val debtAging: DebtAging = DebtAging(),
    val salesProfitLast7Days: List<SalesProfitDayEntry> = emptyList(),
    val monthlyReportTotals: FinancialReportTotals = FinancialReportTotals(),
    val monthlyDailySalesProfit: List<DailySalesProfit> = emptyList(),
    val monthlyReportMonth: Int = -1,
    val monthlyReportYear: Int = -1,
    val isMonthlyReportLoading: Boolean = true,
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDay: Int? = null,
    val dayTotals: Map<Int, Double> = emptyMap(),
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val selectedDaySummary: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0),
    val todayAnalysis: TimeOfDayAnalysis = TimeOfDayAnalysis(),
    val isLoading: Boolean = true
) {
    val isMonthlyReportReady: Boolean
        get() = !isMonthlyReportLoading &&
            monthlyReportMonth == calendarMonth &&
            monthlyReportYear == calendarYear
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val txRepo: TransactionRepository,
    private val customerRepo: CustomerRepository,
    private val reportsRepo: ReportsRepository
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.MONTH)
    private val _calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _calendarYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedDay = MutableStateFlow<Int?>(null)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private data class PeriodFinancials(
        val period: ReportPeriod,
        val totals: FinancialReportTotals
    )

    private data class AdvancedAnalyticsState(
        val netProfit: Double,
        val inventoryValue: InventoryValue,
        val debtAging: DebtAging,
        val salesProfitLast7Days: List<SalesProfitDayEntry>,
        val monthlyReport: CalendarReportState
    )

    private data class CalendarReportState(
        val month: Int,
        val year: Int,
        val totals: FinancialReportTotals,
        val dailySalesProfit: List<DailySalesProfit>
    )

    private val periodFinancials = _period.flatMapLatest { period ->
        val (start, end) = periodRange(period)
        reportsRepo.observeFinancialReportTotals(start, end)
            .map { totals -> PeriodFinancials(period, totals) }
    }

    private val lastSevenDaysSalesProfit = reportsRepo
        .observeDailySalesProfit(
            startDate = lastSevenDaysStart(),
            endDate = todayEnd(),
            timezoneOffsetMillis = currentTimezoneOffsetMillis()
        )
        .map(::fillLastSevenDays)

    private val debtAging = reportsRepo.observeDebtAging(
        oneWeekAgo = daysAgo(7),
        oneMonthAgo = monthsAgo(1),
        threeMonthsAgo = monthsAgo(3)
    )

    private val calendarReport = combine(_calendarMonth, _calendarYear) { month, year ->
        month to year
    }.flatMapLatest { (month, year) ->
        val (start, end) = monthRange(month, year)
        combine(
            reportsRepo.observeFinancialReportTotals(start, end),
            reportsRepo.observeDailySalesProfit(
                startDate = start,
                endDate = end,
                timezoneOffsetMillis = TimeZone.getDefault().getOffset(start).toLong()
            )
        ) { totals, daily ->
            CalendarReportState(month, year, totals, daily)
        }
    }

    init {
        observeBaseReports()
        observeAdvancedAnalytics()
    }

    private fun observeBaseReports() {
        viewModelScope.launch {
            combine(
                txRepo.getAllTransactions(),
                periodFinancials,
                _calendarMonth,
                _calendarYear,
                _selectedDay
            ) { transactions, financials, month, year, selectedDay ->
                buildBaseState(
                    transactions = transactions,
                    periodFinancials = financials,
                    month = month,
                    year = year,
                    selectedDay = selectedDay
                )
            }.collect { baseState ->
                _uiState.update { current ->
                    val monthlyReportMatches =
                        current.monthlyReportMonth == baseState.calendarMonth &&
                            current.monthlyReportYear == baseState.calendarYear
                    baseState.copy(
                        isLoading = false,
                        netProfit = current.netProfit,
                        inventoryCostValue = current.inventoryCostValue,
                        inventorySaleValue = current.inventorySaleValue,
                        debtAging = current.debtAging,
                        salesProfitLast7Days = current.salesProfitLast7Days,
                        monthlyReportTotals = if (monthlyReportMatches) {
                            current.monthlyReportTotals
                        } else {
                            FinancialReportTotals()
                        },
                        monthlyDailySalesProfit = if (monthlyReportMatches) {
                            current.monthlyDailySalesProfit
                        } else {
                            emptyList()
                        },
                        monthlyReportMonth = if (monthlyReportMatches) {
                            current.monthlyReportMonth
                        } else {
                            -1
                        },
                        monthlyReportYear = if (monthlyReportMatches) {
                            current.monthlyReportYear
                        } else {
                            -1
                        },
                        isMonthlyReportLoading = !monthlyReportMatches
                    )
                }
            }
        }
    }

    private fun observeAdvancedAnalytics() {
        viewModelScope.launch {
            combine(
                periodFinancials,
                reportsRepo.observeInventoryValue(),
                debtAging,
                lastSevenDaysSalesProfit,
                calendarReport
            ) { financials, inventoryValue, debtAgingValue, salesProfit, monthlyReport ->
                AdvancedAnalyticsState(
                    netProfit = financials.totals.netProfit,
                    inventoryValue = inventoryValue,
                    debtAging = debtAgingValue,
                    salesProfitLast7Days = salesProfit,
                    monthlyReport = monthlyReport
                )
            }.collect { analytics ->
                _uiState.update { current ->
                    current.copy(
                        netProfit = analytics.netProfit,
                        inventoryCostValue = analytics.inventoryValue.costValue,
                        inventorySaleValue = analytics.inventoryValue.saleValue,
                        debtAging = analytics.debtAging,
                        salesProfitLast7Days = analytics.salesProfitLast7Days,
                        monthlyReportTotals = analytics.monthlyReport.totals,
                        monthlyDailySalesProfit = analytics.monthlyReport.dailySalesProfit,
                        monthlyReportMonth = analytics.monthlyReport.month,
                        monthlyReportYear = analytics.monthlyReport.year,
                        isMonthlyReportLoading = false
                    )
                }
            }
        }
    }

    private fun buildBaseState(
        transactions: List<Transaction>,
        periodFinancials: PeriodFinancials,
        month: Int,
        year: Int,
        selectedDay: Int?
    ): ReportsUiState {
        val period = periodFinancials.period
        val (start, end) = periodRange(period)
        val filtered = transactions.filter { it.date in start..end }

        val dayTotals = buildDayTotals(transactions, month, year)

        val selectedDayTx = if (selectedDay != null) {
            transactionsForDay(transactions, selectedDay, month, year)
        } else emptyList()

        val selTotal = selectedDayTx.sumOf { it.amount }
        val selPaid = selectedDayTx.filter { it.isPaid }.sumOf { it.amount }
        val selUnpaid = selTotal - selPaid

        val todayAnalysis = if (selectedDay != null) {
            buildDayAnalysis(transactions, selectedDay, month, year)
        } else {
            buildTodayAnalysis(transactions)
        }

        return buildState(filtered, transactions, period).copy(
            totalAmount = periodFinancials.totals.totalSales,
            calendarMonth = month,
            calendarYear = year,
            selectedDay = selectedDay,
            dayTotals = dayTotals,
            selectedDayTransactions = selectedDayTx,
            selectedDaySummary = Triple(selTotal, selPaid, selUnpaid),
            todayAnalysis = todayAnalysis
        )
    }

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
    }

    fun selectDay(day: Int) {
        _selectedDay.value = if (_selectedDay.value == day) null else day
    }

    fun prevMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _calendarYear.value)
            set(Calendar.MONTH, _calendarMonth.value)
            add(Calendar.MONTH, -1)
        }
        _calendarMonth.value = cal.get(Calendar.MONTH)
        _calendarYear.value = cal.get(Calendar.YEAR)
        _selectedDay.value = null
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _calendarYear.value)
            set(Calendar.MONTH, _calendarMonth.value)
            add(Calendar.MONTH, 1)
        }
        _calendarMonth.value = cal.get(Calendar.MONTH)
        _calendarYear.value = cal.get(Calendar.YEAR)
        _selectedDay.value = null
    }

    fun buildMonthlyReportData(storeName: String): ReportPdfGenerator.MonthlyReportPdfData {
        val state = _uiState.value
        check(state.isMonthlyReportReady)
        return ReportPdfGenerator.MonthlyReportPdfData(
            storeName = storeName,
            month = state.calendarMonth,
            year = state.calendarYear,
            totalSales = state.monthlyReportTotals.totalSales,
            netProfit = state.monthlyReportTotals.netProfit,
            inventoryCostValue = state.inventoryCostValue,
            inventorySaleValue = state.inventorySaleValue,
            debtAging = state.debtAging,
            dailySalesProfit = state.monthlyDailySalesProfit
        )
    }

    private fun buildDayTotals(all: List<Transaction>, month: Int, year: Int): Map<Int, Double> {
        val cal = Calendar.getInstance()
        return all
            .filter { tx ->
                cal.timeInMillis = tx.date
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }
            .groupBy { tx ->
                cal.timeInMillis = tx.date
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, txList) -> txList.sumOf { it.amount } }
    }

    private fun transactionsForDay(all: List<Transaction>, day: Int, month: Int, year: Int): List<Transaction> {
        val cal = Calendar.getInstance()
        return all.filter { tx ->
            cal.timeInMillis = tx.date
            cal.get(Calendar.DAY_OF_MONTH) == day &&
                cal.get(Calendar.MONTH) == month &&
                cal.get(Calendar.YEAR) == year
        }.sortedByDescending { it.date }
    }

    private fun buildTodayAnalysis(all: List<Transaction>): TimeOfDayAnalysis {
        val start = todayStart()
        val end = todayEnd()
        return buildAnalysisForRange(all, start, end)
    }

    private fun buildDayAnalysis(all: List<Transaction>, day: Int, month: Int, year: Int): TimeOfDayAnalysis {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return buildAnalysisForRange(all, start, end)
    }

    private fun buildAnalysisForRange(all: List<Transaction>, start: Long, end: Long): TimeOfDayAnalysis {
        val cal = Calendar.getInstance()
        var morning = 0.0
        var afternoon = 0.0
        var evening = 0.0
        all.filter { it.date in start..end }.forEach { tx ->
            cal.timeInMillis = tx.date
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 6..11 -> morning += tx.amount
                in 12..17 -> afternoon += tx.amount
                in 18..23 -> evening += tx.amount
            }
        }
        return TimeOfDayAnalysis(morning, afternoon, evening)
    }

    private fun fillLastSevenDays(rows: List<DailySalesProfit>): List<SalesProfitDayEntry> {
        val byDay = rows.associateBy { it.dayStartMillis }
        val dayFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            timeInMillis = lastSevenDaysStart()
        }
        return (0 until 7).map {
            val dayStart = cal.timeInMillis
            val row = byDay[dayStart]
            val entry = SalesProfitDayEntry(
                label = dayFmt.format(Date(dayStart)),
                sales = row?.sales ?: 0.0,
                profit = row?.profit ?: 0.0
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
            entry
        }
    }

    private fun lastSevenDaysStart(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        return cal.timeInMillis
    }

    private fun daysAgo(days: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -days)
    }.timeInMillis

    private fun monthsAgo(months: Int): Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, -months)
    }.timeInMillis

    private fun currentTimezoneOffsetMillis(): Long =
        TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()

    private fun monthRange(month: Int, year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return start to calendar.timeInMillis
    }

    private fun periodRange(p: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (p) {
            ReportPeriod.TODAY -> todayStart() to todayEnd()
            ReportPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
        }
    }

    private fun buildState(filtered: List<Transaction>, all: List<Transaction>, period: ReportPeriod): ReportsUiState {
        val total = filtered.sumOf { it.amount }
        val paid = filtered.filter { it.isPaid }.sumOf { it.amount }
        val unpaid = total - paid

        val dayFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dailyMap = mutableMapOf<String, Pair<Double, Double>>()
        filtered.forEach { tx ->
            val label = dayFmt.format(Date(tx.date))
            val (t, p) = dailyMap.getOrDefault(label, 0.0 to 0.0)
            dailyMap[label] = (t + tx.amount) to (p + if (tx.isPaid) tx.amount else 0.0)
        }
        val dailySales = dailyMap.entries
            .sortedBy { dayFmt.parse(it.key)?.time ?: 0L }
            .map { DaySalesEntry(it.key, it.value.first, it.value.second) }

        val pmMap = mutableMapOf<String, Double>()
        filtered.forEach { tx ->
            val name = tx.paymentMethodName.ifEmpty { "أخرى" }
            pmMap[name] = pmMap.getOrDefault(name, 0.0) + tx.amount
        }
        val paymentShares = pmMap.entries
            .sortedByDescending { it.value }
            .map { PaymentShare(it.key, it.value) }

        val spenderMap = mutableMapOf<String, Double>()
        filtered.forEach { tx ->
            spenderMap[tx.customerName] = spenderMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topSpenders = spenderMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { CustomerRank(it.key, it.value) }

        val debtMap = mutableMapOf<String, Double>()
        all.filter { !it.isPaid }.forEach { tx ->
            debtMap[tx.customerName] = debtMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topDebtors = debtMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { CustomerRank(it.key, it.value) }

        return ReportsUiState(
            period = period,
            totalAmount = total,
            paidAmount = paid,
            unpaidAmount = unpaid,
            txCount = filtered.size,
            filteredTransactions = filtered,
            dailySales = dailySales,
            paymentShares = paymentShares,
            topSpenders = topSpenders,
            topDebtors = topDebtors
        )
    }
}
