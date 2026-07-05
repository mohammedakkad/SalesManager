package com.trader.salesmanager.ui.activation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

sealed class MerchantEvent {
    object Disabled : MerchantEvent()
    object Deleted : MerchantEvent()
    object Expired : MerchantEvent()
    data class ExpiryWarning(val daysLeft: Long) : MerchantEvent()
}

class MerchantWatcherViewModel(
    private val activationRepo: ActivationRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _event = MutableSharedFlow<MerchantEvent>()
    val event: SharedFlow<MerchantEvent> = _event.asSharedFlow()

    private val _expiryBanner = MutableStateFlow<Long?>(null)
    val expiryBanner: StateFlow<Long?> = _expiryBanner.asStateFlow()

    init {
        observeTierChanges()
        observeActivationStatus()
    }

    private fun observeTierChanges() {
        viewModelScope.launch {
            activationRepo.observeMerchantTier().collect { tier ->
                FeatureFlags.applyTier(tier)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeActivationStatus() {
        viewModelScope.launch {
            activationRepo.observeMerchantCode()
                .distinctUntilChanged()
                .flatMapLatest { code ->
                    if (code.isBlank()) emptyFlow()
                    else activationRepo.observeMerchantStatus()
                }
                .collect { status ->
                    when (status) {
                        null -> {
                            activationRepo.deactivate()
                            _event.emit(MerchantEvent.Deleted)
                        }
                        MerchantStatus.DISABLED -> {
                            activationRepo.deactivate()
                            _event.emit(MerchantEvent.Disabled)
                        }
                        MerchantStatus.EXPIRED -> {
                            activationRepo.deactivate()
                            _event.emit(MerchantEvent.Expired)
                        }
                        MerchantStatus.ACTIVE -> Unit
                    }
                }
        }
    }
}
