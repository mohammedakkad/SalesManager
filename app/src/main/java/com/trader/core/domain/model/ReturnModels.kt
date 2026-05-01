package com.trader.core.domain.model

import java.util.UUID

// ── نوع الإرجاع ────────────────────────────────────────────────
enum class ReturnType {
    FULL,       // إرجاع كامل للفاتورة
    PARTIAL     // إرجاع جزئي (يتطلب Premium)
}

// ── حالة الإرجاع ────────────────────────────────────────────────
enum class ReturnStatus {
    PENDING,    // محفوظ محلياً لم يُرفع
    SYNCED      // تم الرفع لـ Firebase
}

// ── فاتورة الإرجاع الرئيسية ─────────────────────────────────────
data class ReturnInvoice(
    val id: String = UUID.randomUUID().toString(),
    val originalTransactionId: Long,    // ← رابط بالعملية الأصلية
    val merchantId: String = "",
    val returnType: ReturnType = ReturnType.FULL,
    val totalRefund: Double = 0.0,      // إجمالي المبلغ المُسترد
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: ReturnStatus = ReturnStatus.PENDING
)

// ── صنف واحد ضمن فاتورة الإرجاع ────────────────────────────────
data class ReturnItem(
    val id: String = UUID.randomUUID().toString(),
    val returnInvoiceId: String,
    val productId: String,              // snapshot — قد يكون محذوفاً
    val productName: String,            // snapshot — يُعرض حتى لو حُذف المنتج
    val unitId: String,
    val unitLabel: String,
    val originalQuantity: Double,       // الكمية الأصلية المُباعة
    val returnedQuantity: Double,       // الكمية المُرجَعة (≤ originalQuantity)
    val pricePerUnit: Double,           // سعر البيع وقت العملية
    val costPricePerUnit: Double = 0.0, // سعر الشراء — لحساب الربح الضائع
    val totalRefund: Double = returnedQuantity * pricePerUnit,
    val lostProfit: Double =            // الربح الضائع بسبب الإرجاع
        if (costPricePerUnit > 0) returnedQuantity * (pricePerUnit - costPricePerUnit) else 0.0
)

// ── حالة UI لمعالجة الإرجاع ────────────────────────────────────
sealed class ReturnUiState {
    object Idle    : ReturnUiState()
    object Loading : ReturnUiState()
    data class Success(val returnInvoice: ReturnInvoice) : ReturnUiState()
    data class Error(val message: String) : ReturnUiState()
    // Feature Flag — المستخدم على الخطة المجانية
    object PartialReturnLocked : ReturnUiState()
}
