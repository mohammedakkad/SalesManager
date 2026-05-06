package com.trader.salesmanager.ui.activation

data class ActivationUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.NONE,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showNoInternetSnackbar: Boolean = false
)

enum class LoadingType { NONE, ACTIVATING_CODE, REGISTERING_FREE }