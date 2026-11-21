package com.trader.core.domain.repository

import com.trader.core.domain.model.StockMovement
import com.trader.core.domain.model.MovementType
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    // ── حركات المخزون ────────────────────────────────────────────
    fun getMovementsForProduct(productId: String, unitId: String): Flow<List<StockMovement>>
    fun getMovementsForTransaction(transactionId: Long): Flow<List<StockMovement>>

    // ── خصم / إضافة ──────────────────────────────────────────────
    /**
     * يخصم كمية من وحدة معينة ويسجل حركة.
     * يُستخدم عند البيع.
     */
    suspend fun deductStock(
        productId: String,
        unitId: String,
        quantity: Double,
        transactionId: Long,
        productName: String,
        unitLabel: String
    )

    /**
     * يُرجع كمية لوحدة معينة ويسجل حركة.
     * يُستخدم عند إلغاء/حذف عملية.
     */
    suspend fun returnStock(
        productId: String,
        unitId: String,
        quantity: Double,
        transactionId: Long,
        productName: String,
        unitLabel: String
    )

    /**
     * تعديل يدوي — إضافة أو خصم.
     * quantity موجب = إضافة، سالب = خصم.
     */
    suspend fun manualAdjust(
        productId: String,
        unitId: String,
        quantity: Double,
        type: MovementType,
        productName: String,
        unitLabel: String,
        note: String = ""
    )

    // ── مزامنة ───────────────────────────────────────────────────
    suspend fun syncPendingMovements()
}
