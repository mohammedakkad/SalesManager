package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.core.domain.model.CashBoxMovement
import com.trader.core.domain.model.CashBoxMovementType
import com.trader.core.domain.model.SyncStatus

@Entity(
    tableName = "cash_box_movements",
    indices = [
        Index("cashBoxId"),
        Index("createdAt"),
        Index("syncStatus")
    ]
)
data class CashBoxMovementEntity(
    @PrimaryKey val id: String,
    val cashBoxId: String,
    val paymentMethodName: String,
    val type: String,
    val amountDelta: Double,
    val balanceAfter: Double,
    val note: String = "",
    val relatedTransactionId: Long? = null,
    val createdAt: Long,
    val merchantId: String = "",
    val syncStatus: String = SyncStatus.PENDING.name
) {
    fun toDomain() = CashBoxMovement(
        id = id,
        cashBoxId = cashBoxId,
        paymentMethodName = paymentMethodName,
        type = runCatching {
            CashBoxMovementType.valueOf(type)
        }.getOrDefault(CashBoxMovementType.MANUAL_ADJUSTMENT),
        amountDelta = amountDelta,
        balanceAfter = balanceAfter,
        note = note,
        relatedTransactionId = relatedTransactionId,
        createdAt = createdAt,
        merchantId = merchantId,
        syncStatus = runCatching {
            SyncStatus.valueOf(syncStatus)
        }.getOrDefault(SyncStatus.PENDING)
    )

    companion object {
        fun fromDomain(m: CashBoxMovement) = CashBoxMovementEntity(
            id = m.id,
            cashBoxId = m.cashBoxId,
            paymentMethodName = m.paymentMethodName,
            type = m.type.name,
            amountDelta = m.amountDelta,
            balanceAfter = m.balanceAfter,
            note = m.note,
            relatedTransactionId = m.relatedTransactionId,
            createdAt = m.createdAt,
            merchantId = m.merchantId,
            syncStatus = m.syncStatus.name
        )
    }
}
