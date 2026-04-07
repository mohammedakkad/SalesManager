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
    val isLoading: Boolean = true
)

class ReportsViewModel(
    private val txRepo: TransactionRepository,
    private val customerRepo: CustomerRepository
) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.MONTH)

    val uiState: StateFlow<ReportsUiState> = combine(
        txRepo.getAllTransactions(),
        _period
    ) { transactions, period ->
        val (start, end) = periodRange(period)
        val filtered = transactions.filter { it.date in start..end }
        buildState(filtered, transactions, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun setPeriod(p: ReportPeriod) { _period.value = p }

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
        val total   = filtered.sumOf { it.amount }
        val paid    = filtered.filter { it.isPaid }.sumOf { it.amount }
        val unpaid  = total - paid

        // Daily sales for line + bar chart
        val dayFmt  = SimpleDateFormat("dd/MM", Locale.getDefault())
        val dailyMap = mutableMapOf<String, Pair<Double, Double>>() // label → (total, paid)
        filtered.forEach { tx ->
            val label = dayFmt.format(Date(tx.date))
            val (t, p) = dailyMap.getOrDefault(label, 0.0 to 0.0)
            dailyMap[label] = (t + tx.amount) to (p + if (tx.isPaid) tx.amount else 0.0)
        }
        val dailySales = dailyMap.entries
            .sortedBy { SimpleDateFormat("dd/MM", Locale.getDefault()).parse(it.key)?.time ?: 0L }
            .map { DaySalesEntry(it.key, it.value.first, it.value.second) }

        // Payment method distribution
        val pmMap = mutableMapOf<String, Double>()
        filtered.forEach { tx ->
            val name = tx.paymentMethodName.ifEmpty { "أخرى" }
            pmMap[name] = pmMap.getOrDefault(name, 0.0) + tx.amount
        }
        val paymentShares = pmMap.entries.sortedByDescending { it.value }
            .map { PaymentShare(it.key, it.value) }

        // Top 5 spenders (by total purchase)
        val spenderMap = mutableMapOf<String, Double>()
        filtered.forEach { tx ->
            spenderMap[tx.customerName] = spenderMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topSpenders = spenderMap.entries.sortedByDescending { it.value }.take(5)
            .map { CustomerRank(it.key, it.value) }

        // Top 5 debtors (unpaid transactions across ALL time)
        val debtMap = mutableMapOf<String, Double>()
        all.filter { !it.isPaid }.forEach { tx ->
            debtMap[tx.customerName] = debtMap.getOrDefault(tx.customerName, 0.0) + tx.amount
        }
        val topDebtors = debtMap.entries.sortedByDescending { it.value }.take(5)
            .map { CustomerRank(it.key, it.value) }

        return ReportsUiState(
            period = period, totalAmount = total, paidAmount = paid, unpaidAmount = unpaid,
            txCount = filtered.size, dailySales = dailySales,
            paymentShares = paymentShares, topSpenders = topSpenders,
            topDebtors = topDebtors, isLoading = false
        )
    }
}
