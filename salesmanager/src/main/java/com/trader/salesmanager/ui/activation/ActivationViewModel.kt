package com.trader.salesmanager.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.model.StartupStatus
import com.trader.core.util.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class StartupState {
    object Checking : StartupState()
    object Proceed  : StartupState()
    object NeedActivation : StartupState()
    data class Blocked(val message: String, val canRetry: Boolean = false) : StartupState()
}

class ActivationViewModel(
    private val repo: ActivationRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private val _startupState = MutableStateFlow<StartupState>(StartupState.Checking)
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    val merchantStatus: StateFlow<MerchantStatus?> = repo.observeMerchantStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { checkStartup() }

    fun checkStartup() {
        _startupState.value = StartupState.Checking
        viewModelScope.launch {
            val result = repo.verifyStatusOnStartup()
            _startupState.value = when (result) {
                StartupStatus.ACTIVE        -> StartupState.Proceed
                StartupStatus.OFFLINE       -> StartupState.Proceed
                StartupStatus.NOT_ACTIVATED -> StartupState.NeedActivation
                StartupStatus.DISABLED      -> StartupState.Blocked(
                    message = "تم تعطيل حسابك من قِبل الإدارة.\nتواصل مع الإدارة لإعادة التفعيل.",
                    canRetry = false
                )
                StartupStatus.EXPIRED       -> StartupState.Blocked(
                    message = "انتهت مدة اشتراكك.\nتواصل مع الإدارة لتجديد الاشتراك.",
                    canRetry = false
                )
                StartupStatus.DELETED       -> StartupState.Blocked(
                    message = "كود التفعيل غير موجود.\nتواصل مع الإدارة للحصول على كود جديد.",
                    canRetry = true
                )
            }
        }
    }

    fun updateCode(code: String) = _uiState.update { it.copy(code = code, error = null) }

    fun activate() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) { _uiState.update { it.copy(error = "أدخل كود التفعيل") }; return }
        if (!networkMonitor.isOnline()) {
            _uiState.update { it.copy(showNoInternetSnackbar = true) }; return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val valid = repo.validateCode(code)
            if (valid) {
                repo.saveActivationStatus(activated = true, code = code)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "كود التفعيل غير صحيح أو الحساب معطل"
                )}
            }
        }
    }

    fun deactivate() {
        viewModelScope.launch { repo.deactivate(); _startupState.value = StartupState.NeedActivation }
    }

    fun onNoInternetSnackbarShown() = _uiState.update { it.copy(showNoInternetSnackbar = false) }
}
