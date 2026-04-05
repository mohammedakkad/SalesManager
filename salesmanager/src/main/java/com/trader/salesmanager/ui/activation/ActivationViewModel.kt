package com.trader.salesmanager.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.domain.repository.ActivationRepository
import com.trader.salesmanager.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivationViewModel(
    private val repo: ActivationRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivationUiState())
    val uiState: StateFlow<ActivationUiState> = _uiState.asStateFlow()

    private val _isActivated = MutableStateFlow<Boolean?>(null)
    val isActivated: StateFlow<Boolean?> = _isActivated.asStateFlow()

    init { checkActivation() }

    private fun checkActivation() {
        viewModelScope.launch {
            _isActivated.value = repo.isActivated()
        }
    }

    fun updateCode(code: String) = _uiState.update { it.copy(code = code, error = null) }

    fun activate() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) {
            _uiState.update { it.copy(error = "أدخل كود التفعيل") }
            return
        }

        if (!networkMonitor.isOnline()) {
            _uiState.update { it.copy(showNoInternetSnackbar = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val isValid = repo.validateCode(code)
            if (isValid) {
                repo.saveActivationStatus(activated = true, code = code)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _isActivated.value = true
            } else {
                _uiState.update { it.copy(isLoading = false, error = "كود التفعيل غير صحيح") }
            }
        }
    }

    fun onNoInternetSnackbarShown() {
        _uiState.update { it.copy(showNoInternetSnackbar = false) }
    }
}
