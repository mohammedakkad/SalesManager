package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.ReturnInvoiceEntity
import com.trader.core.data.local.entity.ReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {

    // ── Insert ─────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnInvoice(entity: ReturnInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    // ── Queries ────────────────────────────────────────────────
    @Query("SELECT * FROM return_invoices WHERE originalTransactionId = :transactionId ORDER BY createdAt DESC")
    fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoiceEntity>>

    @Query("SELECT * FROM return_invoices WHERE merchantId = :merchantId ORDER BY createdAt DESC")
    fun getAllReturns(merchantId: String): Flow<List<ReturnInvoiceEntity>>

    @Query("SELECT * FROM return_items WHERE returnInvoiceId = :returnInvoiceId")
    suspend fun getReturnItems(returnInvoiceId: String): List<ReturnItemEntity>

    // ── المجموع المُرجَع لصنف معين — للتحقق من عدم تجاوز الكمية ──
    @Query("""
        SELECT COALESCE(SUM(ri.returnedQuantity), 0.0)
        FROM return_items ri
        INNER JOIN return_invoices inv ON ri.returnInvoiceId = inv.id
        WHERE inv.originalTransactionId = :transactionId
          AND ri.unitId = :unitId
    """)
    suspend fun totalReturnedForUnit(transactionId: Long, unitId: String): Double

    // ── Sync pending ───────────────────────────────────────────
    @Query("SELECT * FROM return_invoices WHERE syncStatus = 'PENDING'")
    suspend fun getPendingReturns(): List<ReturnInvoiceEntity>

    @Query("UPDATE return_invoices SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}
