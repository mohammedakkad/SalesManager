package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.InventorySession
import com.trader.core.domain.model.InventorySessionItem
import com.trader.core.domain.model.InventoryStatus
import com.trader.core.domain.model.SyncStatus

// ── فاتورة أصناف ─────────────────────────────────────────────────

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val transactionId: Long,
    val productId: String,
    val productName: String,
    val unitId: String,
    val unitLabel: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val totalPrice: Double,
    val merchantId: String,
    val syncStatus: String
) {
    fun toDomain() = InvoiceItem(
        id = id, transactionId = transactionId,
        productId = productId, productName = productName,
        unitId = unitId, unitLabel = unitLabel,
        quantity = quantity, pricePerUnit = pricePerUnit,
        totalPrice = totalPrice, merchantId = merchantId,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun InvoiceItem.toEntity() = InvoiceItemEntity(
    id = id, transactionId = transactionId,
    productId = productId, productName = productName,
    unitId = unitId, unitLabel = unitLabel,
    quantity = quantity, pricePerUnit = pricePerUnit,
    totalPrice = totalPrice, merchantId = merchantId,
    syncStatus = syncStatus.name
)

// ── جرد ──────────────────────────────────────────────────────────

@Entity(tableName = "inventory_sessions")
data class InventorySessionEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val status: String,             // InventoryStatus.name
    val startedAt: Long,
    val finishedAt: Long?,
    val totalAdjustments: Int,
    val syncStatus: String
) {
    fun toDomain() = InventorySession(
        id = id, merchantId = merchantId,
        status = InventoryStatus.valueOf(status),
        startedAt = startedAt, finishedAt = finishedAt,
        totalAdjustments = totalAdjustments,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )
}

fun InventorySession.toEntity() = InventorySessionEntity(
    id = id, merchantId = merchantId,
    status = status.name, startedAt = startedAt,
    finishedAt = finishedAt, totalAdjustments = totalAdjustments,
    syncStatus = syncStatus.name
)

@Entity(tableName = "inventory_session_items")
data class InventorySessionItemEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val productId: String,
    val productName: String,
    val unitId: String,
    val unitLabel: String,
    val systemQuantity: Double,
    val actualQuantity: Double?
) {
    fun toDomain() = InventorySessionItem(
        id = id, sessionId = sessionId,
        productId = productId, productName = productName,
        unitId = unitId, unitLabel = unitLabel,
        systemQuantity = systemQuantity, actualQuantity = actualQuantity
    )
}

fun InventorySessionItem.toEntity() = InventorySessionItemEntity(
    id = id, sessionId = sessionId,
    productId = productId, productName = productName,
    unitId = unitId, unitLabel = unitLabel,
    systemQuantity = systemQuantity, actualQuantity = actualQuantity
)
