package com.trader.salesmanager.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod { TODAY, WEEK, MONTH }

data class DaySalesEntry(val label: String, val total: Double, val paid: Double)
data class CustomerRank(val name: String, val amount: Double)
data class PaymentShare(val name: String, val amount: Double)

// ── تحليل حسب وقت اليوم ─────────────────────────────────────
data class TimeOfDayAnalysis(
    val morningTotal: Double  = 0.0,   // 06:00 – 11:59
    val afternoonTotal: Double = 0.0,  // 12:00 – 17:59
    val eveningTotal: Double  = 0.0    // 18:00 – 23:59
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val txCount: Int = 0,
    val dailySales: List<DaySalesEntry> = emptyList(),
    val paymentShares: List<PaymentShare> = emptyList(),
    val topSpenders: List<CustomerRank> = emptyList(),
    val topDebtors: List<CustomerRank> = emptyList(),

    // ── تقويم ────────────────────────────────────────────────
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH),       // 0-based
    val calendarYear: Int  = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDay: Int?  = null,
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
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val _period      = MutableStateFlow(ReportPeriod.MONTH)
    private val _calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _calendarYear  = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedDay   = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        txRepo.getAllTransactions(),
        _period,
        _calendarMonth,
        _calendarYear,
        _selectedDay
    ) { transactions, period, month, year, selectedDay ->
        val (start, end) = periodRange(period)
        val filtered     = transactions.filter { it.date in start..end }

        // حساب مجاميع كل يوم في الشهر المختار (للتقويم)
        val dayTotals = buildDayTotals(transactions, month, year)

        // عمليات اليوم المحدد
        val selectedDayTx = if (selectedDay != null) {
            transactionsForDay(transactions, selectedDay, month, year)
        } else emptyList()

        val selTotal  = selectedDayTx.sumOf { it.amount }
        val selPaid   = selectedDayTx.filter { it.isPaid }.sumOf { it.amount }
        val selUnpaid = selTotal - selPaid

        // تحليل اليوم (الـ TODAY فقط — من جميع العمليات)
        val todayAnalysis = buildTodayAnalysis(transactions)

        buildState(filtered, transactions, period).copy(
            calendarMonth            = month,
            calendarYear             = year,
            selectedDay              = selectedDay,
            dayTotals                = dayTotals,
            selectedDayTransactions  = selectedDayTx,
            selectedDaySummary       = Triple(selTotal, selPaid, selUnpaid),
            todayAnalysis            = todayAnalysis
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun setPeriod(p: ReportPeriod)   { _period.value = p }
    fun selectDay(day: Int)          { _selectedDay.value = if (_selectedDay.value == day) null else day }
    fun prevMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _calendarYear.value)
            set(Calendar.MONTH, _calendarMonth.value)
            add(Calendar.MONTH, -1)
        }
        _calendarMonth.value = cal.get(Calendar.MONTH)
        _calendarYear.value  = cal.get(Calendar.YEAR)
        _selectedDay.value   = null
    }
    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _calendarYear.value)
            set(Calendar.MONTH, _calendarMonth.value)
            add(Calendar.MONTH, 1)
        }
        _calendarMonth.value = cal.get(Calendar.MONTH)
        _calendarYear.value  = cal.get(Calendar.YEAR)
        _selectedDay.value   = null
    }

    // ── Helpers ───────────────────────────────────────────────
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
            cal.get(Calendar.MONTH)        == month &&
            cal.get(Calendar.YEAR)         == year
        }.sortedByDescending { it.date }
    }

    private fun buildTodayAnalysis(all: List<Transaction>): TimeOfDayAnalysis {
        val start = todayStart()
        val end   = todayEnd()
        val cal   = Calendar.getInstance()
        val today = all.filter { it.date in start..end }

        var morning   = 0.0
        var afternoon = 0.0
        var evening   = 0.0

        today.forEach { tx ->
            cal.timeInMillis = tx.date
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 6..11  -> morning   += tx.amount
                in 12..17 -> afternoon += tx.amount
                in 18..23 -> evening   += tx.amount
            }
        }
        return TimeOfDayAnalysis(morning, afternoon, evening)
    }

    private fun periodRange(p: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (p) {
            ReportPeriod.TODAY -> todayStart() to todayEnd()
            ReportPeriod.WEEK  -> {
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
        val total  = filtered.sumOf { it.amount }
        val paid   = filtered.filter { it.isPaid }.sumOf { it.amount }
        val unpaid = total - paid

        val dayFmt   = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dailyMap = mutableMapOf<String, Pair<Double, Double>>()
        filtered.forEach { tx ->
            val label    = dayFmt.format(Date(tx.date))
            val (t, p)   = dailyMap.getOrDefault(label, 0.0 to 0.0)
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
        val paymentShares = pmMap.entries.sortedByDescending { it.value }
            .map { PaymentShare(it.key, it.value) }

        val spenderMap = mutableMapOf<String, Double>()
        filtered.forEach { tx ->
            spenderMap[tx.customerName] = spenderMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topSpenders = spenderMap.entries.sortedByDescending { it.value }.take(5)
            .map { CustomerRank(it.key, it.value) }

        val debtMap = mutableMapOf<String, Double>()
        all.filter { !it.isPaid }.forEach { tx ->
            debtMap[tx.customerName] = debtMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topDebtors = debtMap.entries.sortedByDescending { it.value }.take(5)
            .map { CustomerRank(it.key, it.value) }

        return ReportsUiState(
            period        = period,
            totalAmount   = total,
            paidAmount    = paid,
            unpaidAmount  = unpaid,
            txCount       = filtered.size,
            dailySales    = dailySales,
            paymentShares = paymentShares,
            topSpenders   = topSpenders,
            topDebtors    = topDebtors,
            isLoading     = false
        )
    }
}
