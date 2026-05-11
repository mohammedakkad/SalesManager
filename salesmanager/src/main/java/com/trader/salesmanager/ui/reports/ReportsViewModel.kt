package com.trader.salesmanager.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.FinancialReportTotals
import com.trader.core.domain.model.InventoryValue
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.ReportsRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod {
    TODAY, WEEK, MONTH
}

data class DaySalesEntry(val label: String, val total: Double, val paid: Double)
data class CustomerRank(val name: String, val amount: Double)
data class PaymentShare(val name: String, val amount: Double)
data class SalesProfitDayEntry(val label: String, val sales: Double, val profit: Double)

// ── تحليل حسب وقت اليوم ─────────────────────────────────────
data class TimeOfDayAnalysis(
    val morningTotal: Double = 0.0, // 06:00 – 11:59
    val afternoonTotal: Double = 0.0, // 12:00 – 17:59
    val eveningTotal: Double = 0.0 // 18:00 – 23:59
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val txCount: Int = 0,
    val filteredTransactions: List<Transaction> = emptyList(), // ✅ كل عمليات الفترة للتصدير
    val dailySales: List<DaySalesEntry> = emptyList(),
    val paymentShares: List<PaymentShare> = emptyList(),
    val topSpenders: List<CustomerRank> = emptyList(),
    val topDebtors: List<CustomerRank> = emptyList(),
    val isAdvancedReportsEnabled: Boolean = false,
    val netProfit: Double = 0.0,
    val inventoryCostValue: Double = 0.0,
    val inventorySaleValue: Double = 0.0,
    val debtAging: DebtAging = DebtAging(),
    val salesProfitLast7Days: List<SalesProfitDayEntry> = emptyList(),

    // ── تقويم ────────────────────────────────────────────────
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-based
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDay: Int? = null,
    /** مجموع مبيعات كل يوم في الشهر الحالي — key = يوم الشهر (1..31) */
    val dayTotals: Map<Int, Double> = emptyMap(),
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val selectedDaySummary: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0), // total, paid, unpaid

    // ── تحليل اليوم ──────────────────────────────────────────
    val todayAnalysis: TimeOfDayAnalysis = TimeOfDayAnalysis(),

    val isLoading: Boolean = true
)

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

    private var hasFetchedAdvancedAnalytics = false

    private data class PeriodFinancials(
        val period: ReportPeriod,
        val totals: FinancialReportTotals
    )

    private data class AdvancedAnalyticsState(
        val netProfit: Double,
        val inventoryValue: InventoryValue,
        val debtAging: DebtAging,
        val salesProfitLast7Days: List<SalesProfitDayEntry>
    )

    private val periodFinancials = _period.flatMapLatest {
        period ->
        val (start, end) = periodRange(period)
        reportsRepo.observeFinancialReportTotals(start, end)
            .map {
                totals -> PeriodFinancials(period, totals)
            }
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

    init {
        observeBaseReports()
        observeFeatureFlags()
    }

    private fun observeFeatureFlags() {
        viewModelScope.launch {
            FeatureFlags.flow.collect {
                flags ->
                // ✅ Always reflect the tier instantly — both up- and down-grades.
                _uiState.update {
                    it.copy(
                        isAdvancedReportsEnabled = flags.isAdvancedReportsEnabled,
                        // Wipe advanced metrics immediately when downgraded.
                        netProfit = if (flags.isAdvancedReportsEnabled) it.netProfit else 0.0,
                        inventoryCostValue = if (flags.isAdvancedReportsEnabled) it.inventoryCostValue else 0.0,
                        inventorySaleValue = if (flags.isAdvancedReportsEnabled) it.inventorySaleValue else 0.0,
                        debtAging = if (flags.isAdvancedReportsEnabled) it.debtAging else DebtAging(),
                        salesProfitLast7Days = if (flags.isAdvancedReportsEnabled) it.salesProfitLast7Days else emptyList()
                    )
                }
                if (flags.isAdvancedReportsEnabled && !hasFetchedAdvancedAnalytics) {
                    hasFetchedAdvancedAnalytics = true
                    observeAdvancedAnalytics()
                }
            }
        }
    }

    private fun observeBaseReports() {
        viewModelScope.launch {
            val baseFlow = combine(
                txRepo.getAllTransactions(),
                periodFinancials,
                _calendarMonth,
                _calendarYear,
                _selectedDay
            ) {
                transactions, financials, month, year, selectedDay ->
                buildBaseState(
                    transactions = transactions,
                    periodFinancials = financials,
                    month = month,
                    year = year,
                    selectedDay = selectedDay
                )
            }

            // ✅ Always derive isAdvancedReportsEnabled from the latest tier — no stale cache.
            combine(baseFlow, FeatureFlags.flow) {
                baseState, flags ->
                baseState to flags.isAdvancedReportsEnabled
            }.collect {
                (baseState, isAdvanced) ->
                _uiState.update {
                    current ->
                    baseState.copy(
                        isLoading = false,
                        isAdvancedReportsEnabled = isAdvanced,
                        netProfit = if (isAdvanced) current.netProfit else 0.0,
                        inventoryCostValue = if (isAdvanced) current.inventoryCostValue else 0.0,
                        inventorySaleValue = if (isAdvanced) current.inventorySaleValue else 0.0,
                        debtAging = if (isAdvanced) current.debtAging else DebtAging(),
                        salesProfitLast7Days = if (isAdvanced) current.salesProfitLast7Days else emptyList()
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
                lastSevenDaysSalesProfit
            ) {
                financials, inventoryValue, debtAgingValue, salesProfit ->
                AdvancedAnalyticsState(
                    netProfit = financials.totals.netProfit,
                    inventoryValue = inventoryValue,
                    debtAging = debtAgingValue,
                    salesProfitLast7Days = salesProfit
                )
            }.collect {
                analytics ->
                _uiState.update {
                    current ->
                    if (!current.isAdvancedReportsEnabled) {
                        current
                    } else {
                        current.copy(
                            netProfit = analytics.netProfit,
                            inventoryCostValue = analytics.inventoryValue.costValue,
                            inventorySaleValue = analytics.inventoryValue.saleValue,
                            debtAging = analytics.debtAging,
                            salesProfitLast7Days = analytics.salesProfitLast7Days
                        )
                    }
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
        val filtered = transactions.filter {
            it.date in start..end
        }

        // حساب مجاميع كل يوم في الشهر المختار (للتقويم)
        val dayTotals = buildDayTotals(transactions, month, year)

        // عمليات اليوم المحدد
        val selectedDayTx = if (selectedDay != null) {
            transactionsForDay(transactions, selectedDay, month, year)
        } else emptyList()

        val selTotal = selectedDayTx.sumOf {
            it.amount
        }
        val selPaid = selectedDayTx.filter {
            it.isPaid
        }.sumOf {
            it.amount
        }
        val selUnpaid = selTotal - selPaid

        // تحليل اليوم — يستخدم اليوم المحدد إن وُجد، وإلا يوم اليوم
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

    // ── Helpers ───────────────────────────────────────────────
    private fun buildDayTotals(all: List<Transaction>, month: Int, year: Int): Map<Int, Double> {
        val cal = Calendar.getInstance()
        return all
        .filter {
            tx ->
            cal.timeInMillis = tx.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }
        .groupBy {
            tx ->
            cal.timeInMillis = tx.date
            cal.get(Calendar.DAY_OF_MONTH)
        }
        .mapValues {
            (_, txList) -> txList.sumOf {
                it.amount
            }
        }
    }

    private fun transactionsForDay(all: List<Transaction>, day: Int, month: Int, year: Int): List<Transaction> {
        val cal = Calendar.getInstance()
        return all.filter {
            tx ->
            cal.timeInMillis = tx.date
            cal.get(Calendar.DAY_OF_MONTH) == day &&
            cal.get(Calendar.MONTH) == month &&
            cal.get(Calendar.YEAR) == year
        }.sortedByDescending {
            it.date
        }
    }

    private fun buildTodayAnalysis(all: List<Transaction>): TimeOfDayAnalysis {
        val start = todayStart()
        val end = todayEnd()
        return buildAnalysisForRange(all, start, end)
    }

    private fun buildDayAnalysis(all: List<Transaction>, day: Int, month: Int, year: Int): TimeOfDayAnalysis {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return buildAnalysisForRange(all, start, end)
    }

    private fun buildAnalysisForRange(all: List<Transaction>, start: Long, end: Long): TimeOfDayAnalysis {
        val cal = Calendar.getInstance()
        var morning = 0.0; var afternoon = 0.0; var evening = 0.0
        all.filter {
            it.date in start..end
        }.forEach {
            tx ->
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
        val byDay = rows.associateBy {
            it.dayStartMillis
        }
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

    private fun periodRange(p: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (p) {
            ReportPeriod.TODAY -> todayStart() to todayEnd()
            ReportPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
        }
    }

    private fun buildState(filtered: List<Transaction>, all: List<Transaction>, period: ReportPeriod): ReportsUiState {
        val total = filtered.sumOf {
            it.amount
        }
        val paid = filtered.filter {
            it.isPaid
        }.sumOf {
            it.amount
        }
        val unpaid = total - paid

        val dayFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dailyMap = mutableMapOf<String, Pair<Double, Double>>()
        filtered.forEach {
            tx ->
            val label = dayFmt.format(Date(tx.date))
            val (t, p) = dailyMap.getOrDefault(label, 0.0 to 0.0)
            dailyMap[label] = (t + tx.amount) to (p + if (tx.isPaid) tx.amount else 0.0)
        }
        val dailySales = dailyMap.entries
        .sortedBy {
            dayFmt.parse(it.key)?.time ?: 0L
        }
        .map {
            DaySalesEntry(it.key, it.value.first, it.value.second)
        }

        val pmMap = mutableMapOf<String, Double>()
        filtered.forEach {
            tx ->
            val name = tx.paymentMethodName.ifEmpty {
                "أخرى"
            }
            pmMap[name] = pmMap.getOrDefault(name, 0.0) + tx.amount
        }
        val paymentShares = pmMap.entries.sortedByDescending {
            it.value
        }
        .map {
            PaymentShare(it.key, it.value)
        }

        val spenderMap = mutableMapOf<String, Double>()
        filtered.forEach {
            tx ->
            spenderMap[tx.customerName] = spenderMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topSpenders = spenderMap.entries.sortedByDescending {
            it.value
        }.take(5)
        .map {
            CustomerRank(it.key, it.value)
        }

        val debtMap = mutableMapOf<String, Double>()
        all.filter {
            !it.isPaid
        }.forEach {
            tx ->
            debtMap[tx.customerName] = debtMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topDebtors = debtMap.entries.sortedByDescending {
            it.value
        }.take(5)
        .map {
            CustomerRank(it.key, it.value)
        }

        return ReportsUiState(
            period = period,
            totalAmount = total,
            paidAmount = paid,
            unpaidAmount = unpaid,
            txCount = filtered.size,
            filteredTransactions = filtered, // ✅
            dailySales = dailySales,
            paymentShares = paymentShares,
            topSpenders = topSpenders,
            topDebtors = topDebtors
        )
    }
}
