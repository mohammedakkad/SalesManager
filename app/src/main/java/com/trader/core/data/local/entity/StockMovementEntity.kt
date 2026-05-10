package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.MovementType
import com.trader.core.domain.model.StockMovement
import com.trader.core.domain.model.SyncStatus

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val unitId: String,
    val unitLabel: String,
    val movementType: String,       // MovementType.name
    val quantity: Double,
    val quantityBefore: Double,
    val quantityAfter: Double,
    val relatedTransactionId: Long?,
    val note: String,
    val merchantId: String,
    val createdAt: Long,
    val syncStatus: String
) {
    fun toDomain() = StockMovement(
        id = id, productId = productId, productName = productName,
        unitId = unitId, unitLabel = unitLabel,
        movementType = MovementType.valueOf(movementType),
        quantity = quantity, quantityBefore = quantityBefore,
        quantityAfter = quantityAfter,
        relatedTransactionId = relatedTransactionId,
        note = note, merchantId = merchantId, createdAt = createdAt,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun StockMovement.toEntity() = StockMovementEntity(
    id = id, productId = productId, productName = productName,
    unitId = unitId, unitLabel = unitLabel,
    movementType = movementType.name,
    quantity = quantity, quantityBefore = quantityBefore,
    quantityAfter = quantityAfter,
    relatedTransactionId = relatedTransactionId,
    note = note, merchantId = merchantId, createdAt = createdAt,
    syncStatus = syncStatus.name
)
