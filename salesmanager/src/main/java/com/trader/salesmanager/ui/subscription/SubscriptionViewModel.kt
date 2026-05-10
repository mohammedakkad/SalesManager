package com.trader.salesmanager.ui.subscription

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.manager.SubscriptionManager
import com.trader.core.data.remote.CloudinaryUploader
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.SubscriptionPaymentMethod
import com.trader.core.domain.model.SubscriptionPlan
import com.trader.core.domain.model.SubscriptionRequest
import com.trader.core.domain.model.SubscriptionState
import com.trader.core.domain.model.SubscriptionStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.util.ImageCompressor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    application: Application,
    private val subscriptionManager: SubscriptionManager,
    private val cloudinaryUploader: CloudinaryUploader,
    private val activationRepository: ActivationRepository,
    private val firebaseSyncService: FirebaseSyncService
) : AndroidViewModel(application) {

    private val _baseUiState = MutableStateFlow(SubscriptionUiState())

    val uiState: StateFlow<SubscriptionUiState> = combine(
        _baseUiState,
        subscriptionManager.configFlow
    ) { state, config ->
        val plans = config.first
        val methods = config.second
        state.copy(
            plans = plans,
            paymentMethods = methods,
            selectedPlan = state.selectedPlan ?: plans.firstOrNull(),
            selectedPaymentMethod = state.selectedPaymentMethod ?: methods.firstOrNull()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionUiState())

    val subscriptionState: StateFlow<SubscriptionState> =
        subscriptionManager.subscriptionState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SubscriptionState()
        )

    init {
        initializeSubscriptionLogic()
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _baseUiState.update { it.copy(selectedPlan = plan) }
    }

    fun selectPaymentMethod(method: SubscriptionPaymentMethod) {
        _baseUiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun onReceiptPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val bytes = ImageCompressor.compress(getApplication(), uri)
                _baseUiState.update { it.copy(receiptImageBytes = bytes) }
            }.onFailure { e ->
                _baseUiState.update { it.copy(error = "فشل تحميل الصورة: ${e.message}") }
            }
        }
    }

    fun submit() {
        val current = uiState.value
        if (!current.canSubmit) return

        viewModelScope.launch {
            _baseUiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val receiptUrl = cloudinaryUploader.uploadReceipt(current.receiptImageBytes!!).getOrThrow()
                val merchantCode = activationRepository.getMerchantCode()
                val request = SubscriptionRequest(
                    merchantCode = merchantCode,
                    planType = current.selectedPlan?.id ?: "",
                    paymentMethod = current.selectedPaymentMethod?.id ?: "",
                    receiptUrl = receiptUrl
                )
                subscriptionManager.pushSubscriptionRequest(merchantCode, request)
                subscriptionManager.markPending()
            }.onSuccess {
                _baseUiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                _baseUiState.update { it.copy(isLoading = false, error = "فشل الإرسال: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _baseUiState.update { it.copy(error = null) }
    }

    private fun initializeSubscriptionLogic() {
        viewModelScope.launch {
            subscriptionManager.syncServerTimeOffset()
            subscriptionManager.syncAdminConfig()
            observeRemoteSubscriptionChanges()
        }
    }

    private fun observeRemoteSubscriptionChanges() {
        viewModelScope.launch {
            val merchantCode = activationRepository.getMerchantCode()
            firebaseSyncService.observeSubscriptionRequestStatus(merchantCode)
                .collect { status ->
                    handleSubscriptionStatusChange(status, merchantCode)
                }
        }
    }

    private suspend fun handleSubscriptionStatusChange(
        status: SubscriptionStatus,
        merchantCode: String
    ) {
        if (status == SubscriptionStatus.APPROVED) {
            subscriptionManager.syncSubscriptionFromRemote(merchantCode)
            _baseUiState.update { it.copy(isSuccess = true) }
        }
    }
}