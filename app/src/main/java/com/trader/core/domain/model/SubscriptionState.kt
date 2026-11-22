package com.trader.core.domain.model

data class SubscriptionState(
    val tier: MerchantTier = MerchantTier.FREE,
    val expiryDate: Long = 0L,
    val isExpired: Boolean = false,
    val isInGracePeriod: Boolean = false,
    val gracePeriodEndsAt: Long = 0L
) {
    val isPending: Boolean
        get() = tier == MerchantTier.PENDING

    val gracePeriodRemainingDays: Int
        get() {
            if (!isInGracePeriod || gracePeriodEndsAt == 0L) return 0
            val remaining = gracePeriodEndsAt - System.currentTimeMillis()
            return (remaining / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        }
}
