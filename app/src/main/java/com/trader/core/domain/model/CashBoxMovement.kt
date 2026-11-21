package com.trader.core.domain.model

data class CashBoxMovement(
    val id: String = "",
    val cashBoxId: String,
    val paymentMethodName: String,
    val type: CashBoxMovementType,
    val amountDelta: Double,
    val balanceAfter: Double,
    val note: String = "",
    val relatedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val merchantId: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class CashBoxMovementType {
    TRANSACTION_EFFECT,
    MANUAL_ADJUSTMENT,
    INITIAL_BALANCE
}

enum class AdjustmentReason {
    CORRECTION,
    COUNT,
    TRANSFER,
    OTHER
}
