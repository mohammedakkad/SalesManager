package com.trader.salesmanager.ui.activation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.FeatureFlags
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.MerchantStatusRepository
import com.trader.salesmanager.service.NotificationService
import kotlinx.coroutines.flow.*
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
    application: Application
) : AndroidViewModel(application) {

    private val _event = MutableSharedFlow<MerchantEvent>()
    val event: SharedFlow<MerchantEvent> = _event.asSharedFlow()

    // In-app banner for expiry warning
    private val _expiryBanner = MutableStateFlow<Long?>(null)
    val expiryBanner: StateFlow<Long?> = _expiryBanner.asStateFlow()

    init {
        observeTierChanges()
    }

    private fun observeTierChanges() {
        viewModelScope.launch {
            activationRepo.observeMerchantTier().collect { tier ->
                // DIP: VM communicates with the Domain logic of FeatureFlags
                FeatureFlags.applyTier(tier)
            }
        }
    }
}