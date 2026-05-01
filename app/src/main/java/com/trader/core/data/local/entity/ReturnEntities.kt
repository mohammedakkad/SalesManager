package com.trader.core.data.local.entity

import androidx.room.*
import com.trader.core.domain.model.*

// ── جدول فواتير الإرجاع ─────────────────────────────────────────
@Entity(
    tableName = "return_invoices",
    indices = [Index("originalTransactionId"), Index("merchantId")]
)
data class ReturnInvoiceEntity(
    @PrimaryKey val id: String,
    val originalTransactionId: Long,
    val merchantId: String,
    val returnType: String,
    val totalRefund: Double,
    val note: String,
    val createdAt: Long,
    val syncStatus: String
) {
    fun toDomain() = ReturnInvoice(
        id                   = id,
        originalTransactionId = originalTransactionId,
        merchantId           = merchantId,
        returnType           = ReturnType.valueOf(returnType),
        totalRefund          = totalRefund,
        note                 = note,
        createdAt            = createdAt,
        syncStatus           = ReturnStatus.valueOf(syncStatus)
    )
}

fun ReturnInvoice.toEntity() = ReturnInvoiceEntity(
    id                   = id,
    originalTransactionId = originalTransactionId,
    merchantId           = merchantId,
    returnType           = returnType.name,
    totalRefund          = totalRefund,
    note                 = note,
    createdAt            = createdAt,
    syncStatus           = syncStatus.name
)

// ── جدول أصناف الإرجاع ──────────────────────────────────────────
@Entity(
    tableName = "return_items",
    foreignKeys = [ForeignKey(
        entity        = ReturnInvoiceEntity::class,
        parentColumns = ["id"],
        childColumns  = ["returnInvoiceId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("returnInvoiceId"), Index("productId")]
)
data class ReturnItemEntity(
    @PrimaryKey val id: String,
    val returnInvoiceId: String,
    val productId: String,
    val productName: String,
    val unitId: String,
    val unitLabel: String,
    val originalQuantity: Double,
    val returnedQuantity: Double,
    val pricePerUnit: Double,
    val costPricePerUnit: Double,
    val totalRefund: Double,
    val lostProfit: Double
) {
    fun toDomain() = ReturnItem(
        id              = id,
        returnInvoiceId = returnInvoiceId,
        productId       = productId,
        productName     = productName,
        unitId          = unitId,
        unitLabel       = unitLabel,
        originalQuantity = originalQuantity,
        returnedQuantity = returnedQuantity,
        pricePerUnit    = pricePerUnit,
        costPricePerUnit = costPricePerUnit,
        totalRefund     = totalRefund,
        lostProfit      = lostProfit
    )
}

fun ReturnItem.toEntity() = ReturnItemEntity(
    id              = id,
    returnInvoiceId = returnInvoiceId,
    productId       = productId,
    productName     = productName,
    unitId          = unitId,
    unitLabel       = unitLabel,
    originalQuantity = originalQuantity,
    returnedQuantity = returnedQuantity,
    pricePerUnit    = pricePerUnit,
    costPricePerUnit = costPricePerUnit,
    totalRefund     = totalRefund,
    lostProfit      = lostProfit
)
