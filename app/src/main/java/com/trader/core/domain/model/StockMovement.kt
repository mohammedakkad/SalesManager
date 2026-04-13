package com.trader.core.domain.model

data class StockMovement(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",       // snapshot
    val unitId: String = "",
    val unitLabel: String = "",         // snapshot
    val movementType: MovementType = MovementType.SALE_OUT,
    val quantity: Double = 0.0,         // موجب = دخول، سالب = خروج
    val quantityBefore: Double = 0.0,
    val quantityAfter: Double = 0.0,
    val relatedTransactionId: Long? = null,
    val note: String = "",
    val merchantId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

enum class MovementType {
    SALE_OUT,           // خصم من بيع
    RETURN_IN,          // رجوع بعد إلغاء عملية
    MANUAL_IN,          // إضافة يدوية
    MANUAL_OUT,         // خصم يدوي
    INVENTORY_ADJUST    // تعديل جرد
}
