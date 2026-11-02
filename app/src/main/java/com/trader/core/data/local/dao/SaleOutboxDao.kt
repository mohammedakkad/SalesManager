package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.SaleOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleOutboxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SaleOutboxEntity): Long
    @Query("SELECT * FROM sale_outbox WHERE eventId = :eventId")
    suspend fun get(eventId: String): SaleOutboxEntity?
    @Query("SELECT * FROM sale_outbox ORDER BY sequence")
    suspend fun all(): List<SaleOutboxEntity>
    @Query("SELECT * FROM sale_outbox WHERE merchantId = :merchant AND state != 'ACKNOWLEDGED' ORDER BY sequence LIMIT 1")
    suspend fun firstUnacknowledged(merchant: String): SaleOutboxEntity?
    @Query("SELECT COALESCE(MAX(aggregateRevision), 0) FROM sale_outbox WHERE merchantId = :merchant AND aggregateId = :aggregateId")
    suspend fun revision(merchant: String, aggregateId: Long): Long
    @Query("SELECT EXISTS(SELECT 1 FROM sale_outbox WHERE aggregateId = :aggregateId)")
    suspend fun manages(aggregateId: Long): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM sale_outbox WHERE state != 'ACKNOWLEDGED')")
    suspend fun hasOutstanding(): Boolean
    @Query("SELECT COUNT(*) FROM sale_outbox WHERE state != 'ACKNOWLEDGED'")
    fun observeOutstanding(): Flow<Int>
    @Query("UPDATE sale_outbox SET state = 'PROCESSING', retryCount = retryCount + 1, leaseOwner = :owner, leaseExpiresAt = :expires, updatedAt = :now WHERE eventId = :eventId")
    suspend fun setClaim(eventId: String, owner: String, expires: Long, now: Long)

    /** Merchant FIFO also serializes sale effects on shared stock units and cash boxes. Conflicts block the queue. */
    @Transaction
    suspend fun claim(merchant: String, owner: String, now: Long, leaseMillis: Long): SaleOutboxEntity? {
        require(owner.isNotBlank() && leaseMillis in 1..3600000)
        val event = firstUnacknowledged(merchant) ?: return null
        if (event.state == "CONFLICTING" || event.nextAttemptAt > now) return null
        if (event.state == "PROCESSING" && (event.leaseExpiresAt ?: Long.MAX_VALUE) > now) return null
        setClaim(event.eventId, owner, now + leaseMillis, now)
        return get(event.eventId)
    }
    @Query("""UPDATE sale_outbox SET state = 'ACKNOWLEDGED', acknowledgedAt = :now, remoteReceipt = :receipt,
        leaseOwner = NULL, leaseExpiresAt = NULL, lastError = NULL, updatedAt = :now
        WHERE eventId = :eventId AND aggregateRevision = :revision AND leaseOwner = :owner
        AND state = 'PROCESSING' AND leaseExpiresAt > :now""")
    suspend fun acknowledge(eventId: String, revision: Long, owner: String, now: Long, receipt: String): Int
    @Query("""UPDATE sale_outbox SET state = :state, nextAttemptAt = :next, lastError = :error,
        leaseOwner = NULL, leaseExpiresAt = NULL, updatedAt = :now
        WHERE eventId = :eventId AND aggregateRevision = :revision AND leaseOwner = :owner AND state = 'PROCESSING'""")
    suspend fun finishAttempt(eventId: String, revision: Long, owner: String, state: String, next: Long, now: Long, error: String): Int
    @Query("DELETE FROM sale_outbox")
    suspend fun deleteAll()
}
