package com.trader.core.domain.model

data class SubscriptionRequest(
    val merchantCode: String = "",
    val planType: String = "",
    val paymentMethod: String = "",
    val receiptUrl: String = "",
    val status: String = SubscriptionStatus.PENDING.name,
    val requestedAt: Long = System.currentTimeMillis()
) {
    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "merchantCode"  to merchantCode,
        "planType"      to planType,
        "paymentMethod" to paymentMethod,
        "receiptUrl"    to receiptUrl,
        "status"        to status,
        "requestedAt"   to requestedAt
    )
}

enum class SubscriptionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
