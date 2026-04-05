package com.trader.salesmanager.ui.transactions.list

import com.trader.salesmanager.domain.model.Transaction

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filterPaid: Boolean? = null,   // null = all, true = paid, false = unpaid
    val isLoading: Boolean = false
)