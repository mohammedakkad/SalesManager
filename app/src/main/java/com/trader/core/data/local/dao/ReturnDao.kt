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
    suspend fun insertAllInvoices(invoices: List<ReturnInvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnItems(items: List<ReturnItemEntity>)

    /**
     * Atomic insert: inserts the invoice and all its items in a single DB transaction.
     * Prevents the Room Flow race condition where observers on return_invoices fire
     * before return_items are written, causing getReturnSummary to calculate 0.0.
     */
    @Transaction
    suspend fun insertReturnWithItems(
        invoice: ReturnInvoiceEntity,
        items: List<ReturnItemEntity>
    ) {
        insertReturnInvoice(invoice)
        insertReturnItems(items)
    }

    // ── Deletion & Sync Cleanup ─────────────────────────────────
    @Query("SELECT id FROM return_invoices")
    suspend fun getAllReturnIds(): List<String>

    @Query("DELETE FROM return_invoices WHERE id = :id")
    suspend fun deleteReturnInvoiceById(id: String)

    // ── Queries ────────────────────────────────────────────────
    @Query("SELECT * FROM return_invoices WHERE originalTransactionId = :transactionId ORDER BY createdAt DESC")
    fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoiceEntity>>

    @Query("SELECT * FROM return_invoices WHERE merchantId = :merchantId ORDER BY createdAt DESC")
    fun getAllReturns(merchantId: String): Flow<List<ReturnInvoiceEntity>>

    @Query("SELECT * FROM return_invoices ORDER BY createdAt ASC")
    suspend fun getAllInvoicesOnce(): List<ReturnInvoiceEntity>

    @Query("SELECT * FROM return_items")
    suspend fun getAllItemsOnce(): List<ReturnItemEntity>

    @Query("DELETE FROM return_invoices")
    suspend fun deleteAllInvoices()

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

    /**
     * ملخص الكمية المُرجَعة لكل unitId في عملية معينة.
     * يُستخدم في getReturnSummary لمعرفة ما أُرجع لكل صنف.
     * Edge Case: إذا لا توجد سجلات → القائمة فارغة (وليس null)
     */
    @Query("""
        SELECT ri.unitId, COALESCE(SUM(ri.returnedQuantity), 0.0) AS total
        FROM return_items ri
        INNER JOIN return_invoices inv ON ri.returnInvoiceId = inv.id
        WHERE inv.originalTransactionId = :transactionId
        GROUP BY ri.unitId
    """)
    suspend fun getReturnedByUnit(transactionId: Long): List<UnitReturnSummary>
}

/** Projection للـ GROUP BY query */
data class UnitReturnSummary(val unitId: String, val total: Double)