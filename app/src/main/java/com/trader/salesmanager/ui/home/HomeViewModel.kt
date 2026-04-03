package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.repository.TransactionRepository
import com.trader.salesmanager.util.DateUtils.todayEnd
import com.trader.salesmanager.util.DateUtils.todayStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: TransactionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadTodaySummary() }

    fun loadTodaySummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val start = todayStart()
            val end   = todayEnd()
            val total  = repo.getTotalAmountByDate(start, end)
            val paid   = repo.getPaidAmountByDate(start, end)
            _uiState.update {
                it.copy(
                    todayTotal   = total,
                    todayPaid    = paid,
                    todayUnpaid  = total - paid,
                    isLoading    = false
                )
            }
        }
    }
}