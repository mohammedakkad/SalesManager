package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.InvoiceItemEntity
import com.trader.core.data.local.entity.InventorySessionEntity
import com.trader.core.data.local.entity.InventorySessionItemEntity
import kotlinx.coroutines.flow.Flow

data class FinancialReportTotalsProjection(
    val totalSales: Double,
    val netProfit: Double
)

data class DailySalesProfitProjection(
    val dayStartMillis: Long,
    val sales: Double,
    val profit: Double
)

// ── فاتورة أصناف ─────────────────────────────────────────────────

@Dao
interface InvoiceItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoice_items ORDER BY transactionId ASC")
    suspend fun getAllOnce(): List<InvoiceItemEntity>

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM invoice_items WHERE transactionId = :transactionId ORDER BY rowid ASC")
    fun getForTransaction(transactionId: Long): Flow<List<InvoiceItemEntity>>

    /** للقراءة الآنية عند تعديل العملية — يُستخدم لمعرفة الأصناف القديمة قبل الحذف */
    @Query("SELECT * FROM invoice_items WHERE transactionId = :transactionId ORDER BY rowid ASC")
    suspend fun getForTransactionOnce(transactionId: Long): List<InvoiceItemEntity>

    @Query("DELETE FROM invoice_items WHERE transactionId = :transactionId")
    suspend fun deleteForTransaction(transactionId: Long)

    @Query("SELECT * FROM invoice_items WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<InvoiceItemEntity>

    @Query("UPDATE invoice_items SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query(
        """
        WITH invoice_totals AS (
            SELECT
                ii.transactionId AS transactionId,
                COALESCE(SUM(ii.quantity * ii.pricePerUnit), 0.0) AS grossSales,
                COALESCE(SUM(ii.quantity * COALESCE(pu.costPrice, 0.0)), 0.0) AS totalCost
            FROM invoice_items ii
            LEFT JOIN product_units pu ON pu.id = ii.unitId
            GROUP BY ii.transactionId
        ),
        sales_totals AS (
            SELECT
                COALESCE(SUM(
                    CASE
                        WHEN invoice_totals.grossSales IS NOT NULL THEN
                            invoice_totals.grossSales +
                            CASE
                                WHEN t.amount > invoice_totals.grossSales
                                    THEN t.amount - invoice_totals.grossSales
                                ELSE 0.0
                            END
                        ELSE t.amount
                    END
                ), 0.0) AS grossSales,
                COALESCE(SUM(
                    CASE
                        WHEN invoice_totals.grossSales IS NOT NULL THEN
                            (invoice_totals.grossSales - invoice_totals.totalCost) +
                            CASE
                                WHEN t.amount > invoice_totals.grossSales
                                    THEN t.amount - invoice_totals.grossSales
                                ELSE 0.0
                            END
                        ELSE t.amount
                    END
                ), 0.0) AS grossProfit
            FROM transactions t
            LEFT JOIN invoice_totals ON invoice_totals.transactionId = t.id
            WHERE t.date BETWEEN :startDate AND :endDate
        ),
        return_totals AS (
            SELECT
                COALESCE(SUM(ri.totalRefund), 0.0) AS refundAmount,
                COALESCE(SUM(ri.lostProfit), 0.0) AS lostProfit
            FROM return_invoices rinv
            INNER JOIN return_items ri ON ri.returnInvoiceId = rinv.id
            WHERE rinv.createdAt BETWEEN :startDate AND :endDate
        )
        SELECT
            sales_totals.grossSales - return_totals.refundAmount AS totalSales,
            sales_totals.grossProfit - return_totals.lostProfit AS netProfit
        FROM sales_totals, return_totals
        """
    )
    fun observeFinancialReportTotals(
        startDate: Long,
        endDate: Long
    ): Flow<FinancialReportTotalsProjection>

    @Query(
        """
        WITH invoice_totals AS (
            SELECT
                ii.transactionId AS transactionId,
                COALESCE(SUM(ii.quantity * ii.pricePerUnit), 0.0) AS grossSales,
                COALESCE(SUM(ii.quantity * COALESCE(pu.costPrice, 0.0)), 0.0) AS totalCost
            FROM invoice_items ii
            LEFT JOIN product_units pu ON pu.id = ii.unitId
            GROUP BY ii.transactionId
        ),
        sales_rows AS (
            SELECT
                ((t.date + :timezoneOffsetMillis) / 86400000) * 86400000 - :timezoneOffsetMillis AS dayStartMillis,
                CASE
                    WHEN invoice_totals.grossSales IS NOT NULL THEN
                        invoice_totals.grossSales +
                        CASE
                            WHEN t.amount > invoice_totals.grossSales
                                THEN t.amount - invoice_totals.grossSales
                            ELSE 0.0
                        END
                    ELSE t.amount
                END AS sales,
                CASE
                    WHEN invoice_totals.grossSales IS NOT NULL THEN
                        (invoice_totals.grossSales - invoice_totals.totalCost) +
                        CASE
                            WHEN t.amount > invoice_totals.grossSales
                                THEN t.amount - invoice_totals.grossSales
                            ELSE 0.0
                        END
                    ELSE t.amount
                END AS profit
            FROM transactions t
            LEFT JOIN invoice_totals ON invoice_totals.transactionId = t.id
            WHERE t.date BETWEEN :startDate AND :endDate
        ),
        return_rows AS (
            SELECT
                ((rinv.createdAt + :timezoneOffsetMillis) / 86400000) * 86400000 - :timezoneOffsetMillis AS dayStartMillis,
                -COALESCE(SUM(ri.totalRefund), 0.0) AS sales,
                -COALESCE(SUM(ri.lostProfit), 0.0) AS profit
            FROM return_invoices rinv
            INNER JOIN return_items ri ON ri.returnInvoiceId = rinv.id
            WHERE rinv.createdAt BETWEEN :startDate AND :endDate
            GROUP BY dayStartMillis
        ),
        all_rows AS (
            SELECT dayStartMillis, sales, profit FROM sales_rows
            UNION ALL
            SELECT dayStartMillis, sales, profit FROM return_rows
        )
        SELECT
            dayStartMillis,
            COALESCE(SUM(sales), 0.0) AS sales,
            COALESCE(SUM(profit), 0.0) AS profit
        FROM all_rows
        GROUP BY dayStartMillis
        ORDER BY dayStartMillis ASC
        """
    )
    fun observeDailySalesProfit(
        startDate: Long,
        endDate: Long,
        timezoneOffsetMillis: Long
    ): Flow<List<DailySalesProfitProjection>>
}

// ── جرد ──────────────────────────────────────────────────────────

@Dao
interface InventoryDao {

    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InventorySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<InventorySessionEntity>)

    @Update
    suspend fun updateSession(session: InventorySessionEntity)

    @Query("SELECT * FROM inventory_sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveSession(): Flow<InventorySessionEntity?>

    @Query("SELECT * FROM inventory_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<InventorySessionEntity>>

    @Query("SELECT * FROM inventory_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessionsOnce(): List<InventorySessionEntity>

    @Query("SELECT * FROM inventory_session_items")
    suspend fun getAllSessionItemsOnce(): List<InventorySessionItemEntity>

    @Query("DELETE FROM inventory_session_items")
    suspend fun deleteAllSessionItems()

    @Query("DELETE FROM inventory_sessions")
    suspend fun deleteAllSessions()

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