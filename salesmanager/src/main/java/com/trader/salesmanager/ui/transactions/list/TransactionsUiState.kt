package com.trader.salesmanager.ui.transactions.list

import com.trader.core.domain.model.Transaction

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filterPaid: Boolean? = null,
    val isLoading: Boolean = false
)