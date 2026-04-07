package com.trader.salesmanager.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.CustomerRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import kotlinx.coroutines.flow.*
import java.util.*

data class DailyStat(val day: Int, val total: Double, val paid: Double)
data class PaymentStat(val name: String, val amount: Double, val color: Long)
data class TopCustomer(val name: String, val total: Double, val debt: Double)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val totalSales: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalUnpaid: Double = 0.0,
    val transactionCount: Int = 0,
    val dailyStats: List<DailyStat> = emptyList(),
    val paymentStats: List<PaymentStat> = emptyList(),
    val topCustomers: List<TopCustomer> = emptyList(),
    val isLoading: Boolean = true
)

enum class ReportPeriod { DAY, WEEK, MONTH }

class ReportsViewModel(
    private val transactionRepo: TransactionRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.MONTH)
    val uiState: StateFlow<ReportsUiState> = _period
        .flatMapLatest { period ->
            val (start, end) = periodRange(period)
            transactionRepo.getTransactionsByDate(start, end).map { transactions ->
                buildState(transactions, period)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun setPeriod(p: ReportPeriod) { _period.value = p }

    private fun periodRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = todayEnd()
        return when (period) {
            ReportPeriod.DAY   -> todayStart() to end
            ReportPeriod.WEEK  -> { cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis to end }
            ReportPeriod.MONTH -> { cal.add(Calendar.DAY_OF_YEAR, -30); cal.timeInMillis to end }
        }
    }

    private fun buildState(txs: List<Transaction>, period: ReportPeriod): ReportsUiState {
        val total   = txs.sumOf { it.amount }
        val paid    = txs.filter { it.isPaid }.sumOf { it.amount }
        val unpaid  = txs.filter { !it.isPaid }.sumOf { it.amount }

        // Daily stats — group by day
        val byDay = txs.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_YEAR)
        }
        val days = when (period) {
            ReportPeriod.DAY   -> 1
            ReportPeriod.WEEK  -> 7
            ReportPeriod.MONTH -> 30
        }
        val cal = Calendar.getInstance()
        val dailyStats = (0 until minOf(days, 30)).map { i ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayKey = c.get(Calendar.DAY_OF_YEAR)
            val dayTxs = byDay[dayKey] ?: emptyList()
            DailyStat(
                day   = days - i,
                total = dayTxs.sumOf { it.amount },
                paid  = dayTxs.filter { it.isPaid }.sumOf { it.amount }
            )
        }.reversed()

        // Payment method breakdown
        val paymentColors = listOf(0xFF10B981L, 0xFF06B6D4L, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFFEF4444L)
        val byMethod = txs.groupBy { it.paymentMethodName.ifEmpty { "غير محدد" } }
        val paymentStats = byMethod.entries.mapIndexed { i, (name, list) ->
            PaymentStat(name, list.sumOf { it.amount }, paymentColors[i % paymentColors.size])
        }.sortedByDescending { it.amount }

        // Top customers
        val byCustomer = txs.groupBy { it.customerId }
        val topCustomers = byCustomer.map { (_, list) ->
            TopCustomer(
                name  = list.first().customerName,
                total = list.sumOf { it.amount },
                debt  = list.filter { !it.isPaid }.sumOf { it.amount }
            )
        }.sortedByDescending { it.total }.take(5)

        return ReportsUiState(
            period           = period,
            totalSales       = total,
            totalPaid        = paid,
            totalUnpaid      = unpaid,
            transactionCount = txs.size,
            dailyStats       = dailyStats,
            paymentStats     = paymentStats,
            topCustomers     = topCustomers,
            isLoading        = false
        )
    }
}