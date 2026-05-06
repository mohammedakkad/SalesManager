package com.trader.salesmanager.ui.activation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.MerchantStatusRepository
import com.trader.salesmanager.service.NotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MerchantEvent {
    object Disabled : MerchantEvent()
    object Deleted  : MerchantEvent()
    object Expired  : MerchantEvent()
    data class ExpiryWarning(val daysLeft: Long) : MerchantEvent()
}

class MerchantWatcherViewModel(
    private val activationRepo: ActivationRepository,
    private val statusRepo: MerchantStatusRepository,
    private val context: Context
) : ViewModel() {

    private val _event = MutableSharedFlow<MerchantEvent>()
    val event: SharedFlow<MerchantEvent> = _event.asSharedFlow()

    // In-app banner for expiry warning
    private val _expiryBanner = MutableStateFlow<Long?>(null)
    val expiryBanner: StateFlow<Long?> = _expiryBanner.asStateFlow()

    init { startWatching() }

    private fun startWatching() {
        viewModelScope.launch {
            val code = activationRepo.getMerchantCode()
            if (code.isEmpty()) return@launch

            // Watch status changes
            launch {
                statusRepo.observeMerchantStatus(code).collect { status ->
                    when (status) {
                        null                   -> { deactivate(); _event.emit(MerchantEvent.Deleted) }
                        MerchantStatus.DISABLED -> { deactivate(); _event.emit(MerchantEvent.Disabled) }
                        MerchantStatus.EXPIRED  -> { deactivate(); _event.emit(MerchantEvent.Expired) }
                        MerchantStatus.ACTIVE   -> { /* all good */ }
                    }
                }
            }

            // Watch expiry countdown
            launch {
                statusRepo.observeExpiryDays(code).collect { daysLeft ->
                    daysLeft ?: return@collect
                    _expiryBanner.value = daysLeft
                    when {
                        daysLeft <= -3L  -> { 
                            // 🔴 انتهت فترة السماح بالكامل (تخطى 3 أيام بالسالب)
                            NotificationService.showExpiryWarning(context, daysLeft)
                            _event.emit(MerchantEvent.Expired)
                            deactivate()
                        }
                        daysLeft in -2L..0L -> { 
                            // 🟡 التاجر الآن يستهلك فترة السماح (سيظهر له شريط تحذيري أحمر في الواجهة)
                            NotificationService.showExpiryWarning(context, daysLeft)
                            _event.emit(MerchantEvent.ExpiryWarning(daysLeft))
                        }
                        daysLeft in 1L..7L  -> { 
                            // 🟢 اقترب الانتهاء (تحذير طبيعي)
                            NotificationService.showExpiryWarning(context, daysLeft)
                            _event.emit(MerchantEvent.ExpiryWarning(daysLeft))
                        }
                    }
                }
            }
        }
    }

    private suspend fun deactivate() {
        activationRepo.saveActivationStatus(false, "")
    }
}