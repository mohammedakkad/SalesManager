package com.trader.admin.ui.subscriptionsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import com.trader.core.domain.repository.AdminSubscriptionSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminSubscriptionUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val methods: List<SubscriptionPaymentMethod> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class AdminSubscriptionViewModel(
    private val repository: AdminSubscriptionSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminSubscriptionUiState())
    val uiState: StateFlow<AdminSubscriptionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSubscriptionConfig()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "فشل تحميل الإعدادات: ${e.message}")
                    }
                }
                .collect { (plans, methods) ->
                    _uiState.update {
                        it.copy(plans = plans, methods = methods, isLoading = false)
                    }
                }
        }
    }

    fun savePlan(plan: SubscriptionPlan) {
        _uiState.update { current ->
            val updated = if (current.plans.any { it.id == plan.id }) {
                current.plans.map { if (it.id == plan.id) plan else it }
            } else {
                current.plans + plan
            }
            syncPlans(updated)
            current.copy(plans = updated)
        }
    }

    fun deletePlan(id: String) {
        _uiState.update { current ->
            if (current.plans.size <= 1) {
                return@update current.copy(error = "يجب الإبقاء على خطة اشتراك واحدة على الأقل")
            }
            val updated = current.plans.filter { it.id != id }
            syncPlans(updated)
            current.copy(plans = updated)
        }
    }

    fun savePaymentMethod(method: SubscriptionPaymentMethod) {
        _uiState.update { current ->
            val updated = if (current.methods.any { it.id == method.id }) {
                current.methods.map { if (it.id == method.id) method else it }
            } else {
                current.methods + method
            }
            syncMethods(updated)
            current.copy(methods = updated)
        }
    }

    fun deletePaymentMethod(id: String) {
        _uiState.update { current ->
            if (current.methods.size <= 1) {
                return@update current.copy(error = "يجب الإبقاء على طريقة دفع واحدة على الأقل")
            }
            val updated = current.methods.filter { it.id != id }
            syncMethods(updated)
            current.copy(methods = updated)
        }
    }

    private fun syncPlans(plans: List<SubscriptionPlan>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { repository.savePlans(plans) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "فشل حفظ الخطط: ${e.message}") }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun syncMethods(methods: List<SubscriptionPaymentMethod>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { repository.savePaymentMethods(methods) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "فشل حفظ طرق الدفع: ${e.message}") }
                }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}