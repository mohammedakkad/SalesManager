package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.TransactionRepository
import com.trader.core.util.DateUtils.todayEnd
import com.trader.core.util.DateUtils.todayStart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getAllTransactions(),
        _merchantId.filter { it.isNotEmpty() }.flatMapLatest { id ->
            chatRepo.getUnreadCount(id, excludeSenderId = id)  // رسائل من الأدمن لم تُقرأ
        }
    ) { transactions, unread ->
        val start = todayStart()
        val end   = todayEnd()
        val todayTx = transactions.filter { it.date in start..end }
        val total   = todayTx.sumOf { it.amount }
        val paid    = todayTx.filter { it.isPaid }.sumOf { it.amount }

        HomeUiState(
            todayTotal           = total,
            todayPaid            = paid,
            todayUnpaid          = total - paid,
            recentTransactions   = transactions
                .sortedByDescending { it.date }
                .take(5),
            unreadChatCount      = unread,
            isLoading            = false
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )
}
