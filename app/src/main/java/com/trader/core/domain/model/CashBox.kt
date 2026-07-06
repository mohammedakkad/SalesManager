package com.trader.core.domain.model

/**
 * صندوق رصيد مرتبط 1:1 بطريقة دفع موجودة.
 * الصندوق ليس طريقة دفع جديدة — هو دفتر رصيد يتحدّث تلقائياً
 * مع كل عملية مدفوعة مرتبطة بطريقة الدفع الخاصة به.
 */
data class CashBox(
    val id: String = "",                    // نفس paymentMethodId — ربط 1:1
    val paymentMethodId: Long,
    val paymentMethodName: String,          // منسوخ للعرض
    val currentBalance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val initialBalanceSetAt: Long? = null,  // null = لم يُحدَّد الرصيد الابتدائي بعد
    val merchantId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    val isInitialized: Boolean get() = initialBalanceSetAt != null
}
