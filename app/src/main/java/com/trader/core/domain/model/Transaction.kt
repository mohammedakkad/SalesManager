package com.trader.core.domain.model

data class Transaction(
    val id: Long = 0,
    val customerId: Long,
    val customerName: String = "",
    val amount: Double,
    val originalAmount: Double = amount,  // ✅ المبلغ الأصلي قبل أي إرجاع
    val isPaid: Boolean,
    val paymentMethodId: Long? = null,
    val paymentMethodName: String = "",
    val paymentType: PaymentType = PaymentType.DEBT,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val hasItems: Boolean = false,
    // ✅ حالة الإرجاع — NONE = لا شيء أُرجع
    val returnStatus: TransactionReturnStatus = TransactionReturnStatus.NONE,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    val isFullyReturned:   Boolean get() = returnStatus == TransactionReturnStatus.FULLY_RETURNED
    val isPartiallyReturned: Boolean get() = returnStatus == TransactionReturnStatus.PARTIALLY_RETURNED
    val hasAnyReturn:      Boolean get() = returnStatus != TransactionReturnStatus.NONE
    /** المبلغ الذي يُعرض مشطوباً في الـ UI إذا تغيّر */
    val amountChanged:     Boolean get() = hasAnyReturn && amount != originalAmount
}
