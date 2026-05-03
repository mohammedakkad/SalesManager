package com.trader.core.domain.model

import java.util.UUID

// ── حالة الإرجاع على مستوى العملية ──────────────────────────────
enum class TransactionReturnStatus {
    /** عملية عادية لم يُرجَع منها شيء */
    NONE,
    /** تم إرجاع جزء من الأصناف */
    PARTIALLY_RETURNED,
    /** تم إرجاع كل الأصناف */
    FULLY_RETURNED
}

// ── نوع الإرجاع ───────────────────────────────────────────────────
enum class ReturnType {
    /** إرجاع كل الفاتورة دفعة واحدة */
    FULL,
    /** إرجاع بعض الأصناف أو كميات جزئية */
    PARTIAL
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
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// ── صنف واحد ضمن فاتورة الإرجاع ─────────────────────────────────
data class ReturnItem(
    val id: String = UUID.randomUUID().toString(),
    val returnInvoiceId: String = "",
    val productId: String = "",
    val productName: String = "",       // snapshot
    val unitId: String = "",
    val unitLabel: String = "",         // snapshot
    val originalQuantity: Double = 0.0, // الكمية الأصلية في الفاتورة
    val returnedQuantity: Double = 0.0, // الكمية المُرجَعة الآن
    val pricePerUnit: Double = 0.0,
    val totalRefund: Double = returnedQuantity * pricePerUnit
) {
    /** للتحقق من صحة البيانات قبل الحفظ */
    fun validate(): Result<Unit> = when {
        returnedQuantity <= 0       -> Result.failure(IllegalArgumentException("الكمية المرجعة يجب أن تكون أكبر من صفر"))
        returnedQuantity > originalQuantity -> Result.failure(IllegalArgumentException("الكمية المرجعة تتجاوز الأصلية"))
        productId.isBlank()         -> Result.failure(IllegalArgumentException("معرف الصنف مطلوب"))
        else                        -> Result.success(Unit)
    }
}

// ── ملخص الإرجاع لعملية معينة (للعرض في الـ UI) ─────────────────
data class ReturnSummary(
    val returnStatus: TransactionReturnStatus,
    val totalRefunded: Double,
    /** الكمية المُرجَعة لكل unitId */
    val returnedByUnit: Map<String, Double>
) {
    companion object {
        val NONE = ReturnSummary(TransactionReturnStatus.NONE, 0.0, emptyMap())
    }
}
