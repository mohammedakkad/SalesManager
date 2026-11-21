package com.trader.core.data.local.entity

import androidx.room.*

@Entity(tableName = "sale_outbox", indices = [
    Index(value = ["eventId"], unique = true),
    Index(value = ["merchantId", "operationId"], unique = true),
    Index(value = ["merchantId", "aggregateId", "aggregateRevision"], unique = true),
    Index(value = ["merchantId", "state", "sequence"]), Index("leaseExpiresAt")
])
data class SaleOutboxEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val eventId: String,
    val operationId: String,
    val merchantId: String,
    val aggregateId: Long,
    val aggregateRevision: Long,
    val eventType: String,
    val payloadVersion: Int,
    val payload: String,
    val state: String = "PENDING",
    val retryCount: Int = 0,
    val nextAttemptAt: Long = 0,
    val leaseOwner: String? = null,
    val leaseExpiresAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val acknowledgedAt: Long? = null,
    val remoteReceipt: String? = null,
    val lastError: String? = null
)
