package com.trader.core.domain.model

data class Transaction(
    val id: Long = 0,
    val customerId: Long,
    val customerName: String = "",
    val amount: Double,
    val isPaid: Boolean,
    val paymentMethodId: Long? = null,
    val paymentMethodName: String = "",
    val paymentType: PaymentType = PaymentType.DEBT,  // ← جديد v2
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val hasItems: Boolean = false,
    // ← جديد v2
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)