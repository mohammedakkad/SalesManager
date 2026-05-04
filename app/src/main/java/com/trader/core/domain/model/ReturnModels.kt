package com.trader.core.domain.model

import java.util.UUID

// ── حالة الإرجاع على مستوى العملية ──────────────────────────────
enum class TransactionReturnStatus {
    NONE,
    PARTIALLY_RETURNED,
    FULLY_RETURNED
}

// ── نوع الإرجاع ───────────────────────────────────────────────────
enum class ReturnType {
    FULL,
    PARTIAL
}

// ── حالة مزامنة الإرجاع — مستقلة عن SyncStatus العام ────────────
enum class ReturnSyncStatus {
    PENDING,
    SYNCED
}

// ── فاتورة الإرجاع ────────────────────────────────────────────────
data class ReturnInvoice(
    val id: String = UUID.randomUUID().toString(),
    val originalTransactionId: Long,
    val merchantId: String = "",
    val returnType: ReturnType = ReturnType.PARTIAL,
    val totalRefund: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // ✅ دمج: يستخدم SyncStatus العام للتوافق مع ReturnRepositoryImpl
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// ── صنف واحد ضمن فاتورة الإرجاع ─────────────────────────────────
data class ReturnItem(
    val id: String = UUID.randomUUID().toString(),
    val returnInvoiceId: String = "",
    val productId: String = "",
    val productName: String = "",
    val unitId: String = "",
    val unitLabel: String = "",
    val originalQuantity: Double = 0.0,
    val returnedQuantity: Double = 0.0,
    val pricePerUnit: Double = 0.0,
    val costPricePerUnit: Double = 0.0,
    val totalRefund: Double = returnedQuantity * pricePerUnit,
    // ✅ الربح الضائع — 0 إذا لم يُحدَّد سعر الشراء (لا نُجبر التاجر)
    val lostProfit: Double =
        if (costPricePerUnit > 0) returnedQuantity * (pricePerUnit - costPricePerUnit) else 0.0
) {
    /** التحقق من صحة البيانات قبل الحفظ — منع Double Return أو إرجاع سالب */
    fun validate(): Result<Unit> = when {
        returnedQuantity <= 0 ->
            Result.failure(IllegalArgumentException("الكمية المرجعة يجب أن تكون أكبر من صفر"))
        returnedQuantity > originalQuantity ->
            Result.failure(IllegalArgumentException("الكمية المرجعة (${returnedQuantity}) تتجاوز الأصلية (${originalQuantity})"))
        productId.isBlank() ->
            Result.failure(IllegalArgumentException("معرف الصنف مطلوب"))
        unitId.isBlank() ->
            Result.failure(IllegalArgumentException("معرف الوحدة مطلوب"))
        else -> Result.success(Unit)
    }
}

// ── ملخص الإرجاع (للعرض في شاشة التفاصيل) ──────────────────────
data class ReturnSummary(
    val returnStatus: TransactionReturnStatus,
    val totalRefunded: Double,
    val returnedByUnit: Map<String, Double>
) {
    companion object {
        val NONE = ReturnSummary(TransactionReturnStatus.NONE, 0.0, emptyMap())
    }
}

// ── حالة UI — للـ ReturnViewModel ────────────────────────────────
sealed class ReturnUiState {
    /** لا توجد عملية إرجاع جارية */
    object Idle : ReturnUiState()
    /** جارٍ المعالجة */
    object Loading : ReturnUiState()
    /** نجح الإرجاع */
    data class Success(val returnInvoice: ReturnInvoice) : ReturnUiState()
    /** فشل — مع رسالة واضحة للتاجر */
    data class Error(val message: String) : ReturnUiState()
    /** الإرجاع الجزئي مقفل (خطة مجانية) — Feature Flag */
    object PartialReturnLocked : ReturnUiState()
}
