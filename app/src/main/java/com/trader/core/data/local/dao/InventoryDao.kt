package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.InvoiceItemEntity
import com.trader.core.data.local.entity.InventorySessionEntity
import com.trader.core.data.local.entity.InventorySessionItemEntity
import kotlinx.coroutines.flow.Flow

// ── فاتورة أصناف ─────────────────────────────────────────────────

@Dao
interface InvoiceItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoice_items WHERE transactionId = :transactionId ORDER BY rowid ASC")
    fun getForTransaction(transactionId: Long): Flow<List<InvoiceItemEntity>>

    @Query("DELETE FROM invoice_items WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)

    @Query("SELECT * FROM invoice_items WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<InvoiceItemEntity>

    @Query("UPDATE invoice_items SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}

// ── جرد ──────────────────────────────────────────────────────────

@Dao
interface InventoryDao {

    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InventorySessionEntity)

    @Update
    suspend fun updateSession(session: InventorySessionEntity)

    @Query("SELECT * FROM inventory_sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveSession(): Flow<InventorySessionEntity?>

    @Query("SELECT * FROM inventory_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<InventorySessionEntity>>

    // Session items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionItems(items: List<InventorySessionItemEntity>)

    @Update
    suspend fun updateSessionItem(item: InventorySessionItemEntity)

    @Query("SELECT * FROM inventory_session_items WHERE sessionId = :sessionId ORDER BY productName ASC")
    fun getSessionItems(sessionId: String): Flow<List<InventorySessionItemEntity>>

    @Query("SELECT * FROM inventory_session_items WHERE sessionId = :sessionId")
    suspend fun getSessionItemsOnce(sessionId: String): List<InventorySessionItemEntity>
}
