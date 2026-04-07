package com.trader.salesmanager.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.util.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActivationViewModel(
    private val repo: ActivationRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private val _isActivated = MutableStateFlow<Boolean?>(null)
    val isActivated: StateFlow<Boolean?> = _isActivated.asStateFlow()

    // Streams merchant status from Firestore — auto-logout on DISABLED/EXPIRED/deleted
    val merchantStatus: StateFlow<MerchantStatus?> = repo.observeMerchantStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { checkActivation() }

    private fun checkActivation() {
        viewModelScope.launch { _isActivated.value = repo.isActivated() }
    }

    fun updateCode(code: String) = _uiState.update { it.copy(code = code, error = null) }

    fun activate() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) { _uiState.update { it.copy(error = "أدخل كود التفعيل") }; return }
        if (!networkMonitor.isOnline()) { _uiState.update { it.copy(showNoInternetSnackbar = true) }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val valid = repo.validateCode(code)
            if (valid) {
                repo.saveActivationStatus(activated = true, code = code)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _isActivated.value = true
            } else {
                _uiState.update { it.copy(isLoading = false, error = "كود التفعيل غير صحيح") }
            }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            repo.deactivate()
            _isActivated.value = false
        }
    }

    fun onNoInternetSnackbarShown() = _uiState.update { it.copy(showNoInternetSnackbar = false) }
}
