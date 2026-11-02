package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/** Local command receipt only. Not an outbox and never dispatched to Firebase. */
@Entity(tableName = "sale_operations", primaryKeys = ["merchantId", "operationId"],
    indices = [Index("transactionId")])
data class SaleOperationEntity(
    val merchantId: String,
    val operationId: String,
    val kind: String,
    val requestHash: String,
    val transactionId: Long
)
