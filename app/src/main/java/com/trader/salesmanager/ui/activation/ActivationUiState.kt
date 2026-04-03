package com.trader.salesmanager.ui.activation

data class ActivationUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)