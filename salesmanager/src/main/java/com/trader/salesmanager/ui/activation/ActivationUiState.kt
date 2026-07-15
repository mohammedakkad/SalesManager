package com.trader.salesmanager.ui.activation

data class ActivationUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val showNoInternetSnackbar: Boolean = false
)
