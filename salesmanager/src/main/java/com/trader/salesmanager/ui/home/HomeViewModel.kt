package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.repository.TransactionRepository
import com.trader.salesmanager.util.DateUtils.todayEnd
import com.trader.salesmanager.util.DateUtils.todayStart
import kotlinx.coroutines.flow.*

class HomeViewModel(private val repo: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repo.getAllTransactions()
        .map { transactions ->
            val start = todayStart()
            val end   = todayEnd()
            val todayTx = transactions.filter { it.date in start..end }
            val total   = todayTx.sumOf { it.amount }
            val paid    = todayTx.filter { it.isPaid }.sumOf { it.amount }
            HomeUiState(
                todayTotal  = total,
                todayPaid   = paid,
                todayUnpaid = total - paid,
                isLoading   = false
            )
        }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(5_000),
            initialValue     = HomeUiState(isLoading = true)
        )
}