package com.trader.core.domain.model

import androidx.annotation.Keep // ✅ أضفنا الاستيراد

@Keep // ✅ هذا السطر السحري يمنع تشفير الكلاس أثناء بناء التطبيق
data class SubscriptionRequest(
    val merchantCode: String = "",
    val planType: String = "",
    val paymentMethod: String = "",
    val receiptUrl: String = "",
    val status: String = SubscriptionStatus.PENDING.name,
    val requestedAt: Long = 0L // ✅ جعلناها 0L كقيمة افتراضية لكي يملأها Firebase بالقيمة الحقيقية براحة
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

@Keep // ✅ حماية الـ Enum أيضاً لكي لا يتغير اسمه ويسبب مشاكل في التحويل
enum class SubscriptionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
