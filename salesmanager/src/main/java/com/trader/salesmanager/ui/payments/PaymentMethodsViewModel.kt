package com.trader.salesmanager.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentMethodsUiState(val methods: List<PaymentMethod> = emptyList())

class PaymentMethodsViewModel(private val repo: PaymentMethodRepository) : ViewModel() {
    val uiState: StateFlow<PaymentMethodsUiState> =
        repo.getAllPaymentMethods().map { PaymentMethodsUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentMethodsUiState())

    fun addMethod(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.insertPaymentMethod(PaymentMethod(name = name)) }
    }
    fun deleteMethod(method: PaymentMethod) {
        viewModelScope.launch { repo.deletePaymentMethod(method) }
    }
}