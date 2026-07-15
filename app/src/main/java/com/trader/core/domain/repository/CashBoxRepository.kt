package com.trader.core.domain.repository

import com.trader.core.domain.model.AdjustmentReason
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.CashBoxMovement
import kotlinx.coroutines.flow.Flow

interface CashBoxRepository {
    fun getAllBoxes(): Flow<List<CashBox>>

    fun getAllMovements(): Flow<List<CashBoxMovement>>

    fun getMovementsForBox(cashBoxId: String): Flow<List<CashBoxMovement>>

    suspend fun getBoxByPaymentMethod(paymentMethodId: Long): CashBox?

    suspend fun setInitialBalance(boxId: String, amount: Double)

    suspend fun adjustBalance(
        boxId: String,
        newAmount: Double,
        note: String?,
        reason: AdjustmentReason
    )

    suspend fun applyTransactionEffect(
        relatedTransactionId: Long?,
        oldPaymentMethodId: Long?, oldAmount: Double, oldWasPaid: Boolean,
        newPaymentMethodId: Long?, newAmount: Double, newWasPaid: Boolean
    )

    suspend fun syncPendingBoxes()
}
