package com.trader.admin.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

data class MerchantNotification(
    val merchantId: String,
    val merchantName: String,
    val message: String,
    val type: NotifType
)

enum class NotifType { EXPIRING_SOON, EXPIRED }

class NotificationsViewModel(private val repo: MerchantAdminRepository) : ViewModel() {

    val notifications: StateFlow<List<MerchantNotification>> = repo.getAllMerchants()
        .map { list -> buildNotifications(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildNotifications(merchants: List<Merchant>): List<MerchantNotification> {
        val now = System.currentTimeMillis()
        val twoDays = TimeUnit.DAYS.toMillis(2)
        val result = mutableListOf<MerchantNotification>()

        merchants.forEach { merchant ->
            when {
                // منتهي الصلاحية
                merchant.status == MerchantStatus.EXPIRED -> {
                    result.add(MerchantNotification(
                        merchantId   = merchant.id,
                        merchantName = merchant.name,
                        message      = "انتهت صلاحية هذا البائع",
                        type         = NotifType.EXPIRED
                    ))
                }
                // سينتهي خلال يومين
                !merchant.isPermanent && merchant.expiryDate != null -> {
                    val expiry = merchant.expiryDate
                        val remaining = expiry!!.toDate().time - now
                    if (remaining in 0..twoDays) {
                        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
                        val msg = if (hours < 24) "ينتهي خلال $hours ساعة!"
                                  else "ينتهي خلال يوم واحد"
                        result.add(MerchantNotification(
                            merchantId   = merchant.id,
                            merchantName = merchant.name,
                            message      = msg,
                            type         = NotifType.EXPIRING_SOON
                        ))
                    }
                }
            }
        }
        return result.sortedBy { it.type.ordinal }
    }
}