package com.trader.admin.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.SubscriptionRequest
import com.trader.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<SubscriptionRequest> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class AdminRequestsViewModel(private val repository: SubscriptionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminRequestsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        repository.getPendingRequests()
            .onEach { list -> _uiState.update { it.copy(requests = list, isLoading = false) } }
            .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun approve(request: SubscriptionRequest) {
        val days = if (request.planType.contains("year", ignoreCase = true)) 365 else 30
        execute(request) { repository.approveRequest(request, days) }
    }

    fun reject(request: SubscriptionRequest) {
        execute(request) { repository.rejectRequest(request) }
    }

    private fun execute(request: SubscriptionRequest, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { action() }
                .onSuccess { _uiState.update { it.copy(isLoading = false, successMessage = "تمت العملية بنجاح") } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}