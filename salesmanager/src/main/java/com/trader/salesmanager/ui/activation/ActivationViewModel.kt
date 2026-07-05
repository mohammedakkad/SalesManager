package com.trader.salesmanager.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.remote.ValidationResult
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.StartupStatus
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class StartupState {
    object Checking : StartupState()
    object Proceed : StartupState()
    object ProceedFree : StartupState()
    object NeedActivation : StartupState()
    data class Blocked(val message: String, val canRetry: Boolean = false) : StartupState()
}

class ActivationViewModel(
    private val repo: ActivationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private val _startupState = MutableStateFlow<StartupState>(StartupState.Checking)
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    val merchantStatus: StateFlow<MerchantStatus?> = repo.observeMerchantStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        checkStartup()
    }

    fun checkStartup() {
        _startupState.value = StartupState.Checking
        viewModelScope.launch {
            val result = repo.verifyStatusOnStartup()
            handleStartupResult(result)
        }
    }

    private suspend fun handleStartupResult(result: StartupStatus) {
        _startupState.value = when (result) {
            StartupStatus.ACTIVE -> {
                val tier = repo.getMerchantTier()
                FeatureFlags.applyTier(tier)
                StartupState.Proceed
            }
            StartupStatus.NOT_ACTIVATED -> StartupState.NeedActivation
            StartupStatus.DISABLED -> StartupState.Blocked(
                "الحساب معطل من قِبل الإدارة",
                canRetry = false
            )
            StartupStatus.OFFLINE -> StartupState.NeedActivation
            else -> StartupState.Blocked("انتهى الاشتراك", canRetry = false)
        }
    }

    fun updateCode(code: String) = _uiState.update {
        it.copy(code = code, error = null)
    }

    fun activate() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null, showNoInternetSnackbar = false)
            }
            when (repo.validateCodeDetailed(code)) {
                ValidationResult.Active -> {
                    repo.saveActivationStatus(activated = true, code = code)
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = true)
                    }
                }

                ValidationResult.Disabled -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "هذا الحساب معطّل. تواصل مع الإدارة."
                        )
                    }
                }

                ValidationResult.Expired -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "انتهت مدة الاشتراك. تواصل مع الإدارة للتجديد."
                        )
                    }
                }

                ValidationResult.NotFound -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "كود التفعيل غير صحيح."
                        )
                    }
                }

                ValidationResult.NetworkError -> {
                    _uiState.update {
                        it.copy(isLoading = false, showNoInternetSnackbar = true)
                    }
                }
            }
        }
    }

    fun onSnackbarShown() = _uiState.update {
        it.copy(error = null, showNoInternetSnackbar = false)
    }

    fun deactivate() {
        viewModelScope.launch {
            repo.deactivate()
            _startupState.value = StartupState.NeedActivation
        }
    }
}
