package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    private val repo: TransactionRepository,
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository
) : ViewModel() {

    private val _merchantId = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _merchantId.value = activationRepo.getMerchantCode()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        repo.getAllTransactions(),
        _merchantId.flatMapLatest { id ->
            if (id.isEmpty()) flowOf(0)
            else chatRepo.getUnreadCount(id, excludeSenderId = id)
        }
    ) { transactions, unread ->
        val todayStart = todayStart()
        val todayEnd   = todayEnd()

        // Yesterday range
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val yStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val yEnd = cal.timeInMillis

        val todayTx     = transactions.filter { it.date in todayStart..todayEnd }
        val yesterdayTx = transactions.filter { it.date in yStart..yEnd }

        val total     = todayTx.sumOf { it.amount }
        val paid      = todayTx.filter { it.isPaid }.sumOf { it.amount }
        val yTotal    = yesterdayTx.sumOf { it.amount }
        val recent    = transactions.sortedByDescending { it.date }.take(5)

        HomeUiState(
            todayTotal           = total,
            todayPaid            = paid,
            todayUnpaid          = total - paid,
            yesterdayTotal       = yTotal,
            recentTransactions   = recent,
            unreadChatCount      = unread,
            isLoading            = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))
}
