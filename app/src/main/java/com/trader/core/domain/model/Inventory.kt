package com.trader.core.domain.model

data class InvoiceItem(
    val id: String = "",
    val transactionId: Long = 0,
    val productId: String = "",
    val productName: String = "",       // snapshot
    val unitId: String = "",
    val unitLabel: String = "",         // snapshot
    val quantity: Double = 0.0,
    val pricePerUnit: Double = 0.0,
    val totalPrice: Double = 0.0,
    val merchantId: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// جلسة جرد كاملة
data class InventorySession(
    val id: String = "",
    val merchantId: String = "",
    val status: InventoryStatus = InventoryStatus.IN_PROGRESS,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val totalAdjustments: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// صنف واحد داخل جلسة الجرد
data class InventorySessionItem(
    val id: String = "",
    val sessionId: String = "",
    val productId: String = "",
    val productName: String = "",
    val unitId: String = "",
    val unitLabel: String = "",
    val systemQuantity: Double = 0.0,
    val actualQuantity: Double? = null
) {
    val difference: Double
        get() = (actualQuantity ?: systemQuantity) - systemQuantity
}

enum class InventoryStatus { IN_PROGRESS, FINISHED }
