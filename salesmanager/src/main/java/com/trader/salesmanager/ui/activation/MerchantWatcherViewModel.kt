package com.trader.salesmanager.ui.activation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class MerchantEvent {
    object Disabled : MerchantEvent()
    object Deleted : MerchantEvent()
    object Expired : MerchantEvent()
    data class ExpiryWarning(val daysLeft: Long) : MerchantEvent()
}

// 1. التعديل هنا: استخدام Application والوراثة من AndroidViewModel
class MerchantWatcherViewModel(
    private val activationRepo: ActivationRepository,
    private val firebaseSyncService: FirebaseSyncService,
    application: Application
) : AndroidViewModel(application) {

    private val _event = MutableSharedFlow<MerchantEvent>()
    val event: SharedFlow<MerchantEvent> = _event.asSharedFlow()

    // In-app banner for expiry warning
    private val _expiryBanner = MutableStateFlow<Long?>(null)
    val expiryBanner: StateFlow<Long?> = _expiryBanner.asStateFlow()
    private var hasSessionBeenConfirmed = false

    init {
        observeTierChanges()
        observeCurrentSession()
    }

    private fun observeTierChanges() {
        viewModelScope.launch {
            activationRepo.observeMerchantTier().collect { tier ->
                // DIP: VM communicates with the Domain logic of FeatureFlags
                FeatureFlags.applyTier(tier)
            }
        }
    }

    private fun observeCurrentSession() {
        viewModelScope.launch {
            var kickoutHandled = false
            combine(
                activationRepo.observeMerchantCode(),
                activationRepo.observeCurrentSessionId()
            ) { merchantCode, sessionId ->
                merchantCode to sessionId
            }
            .filter {
                (merchantCode, sessionId) ->
                merchantCode.isNotBlank() && sessionId.isNotBlank()
            }
            .distinctUntilChanged()
            .onEach {
                hasSessionBeenConfirmed = false
                kickoutHandled = false
            }
            .flatMapLatest {
                (merchantCode, sessionId) ->
                firebaseSyncService
                    .observeCurrentSession(merchantCode, sessionId)
                    .catch {
                        // If network drops, keep session active and wait for next valid emission.
                        emit(true)
                    }
            }
            .collect {
                exists ->
                if (exists) {
                    hasSessionBeenConfirmed = true
                    kickoutHandled = false
                } else if (hasSessionBeenConfirmed && !kickoutHandled) {
                    kickoutHandled = true
                    activationRepo.deactivate()
                    _event.emit(MerchantEvent.Deleted)
                }
            }
        }
    }
}