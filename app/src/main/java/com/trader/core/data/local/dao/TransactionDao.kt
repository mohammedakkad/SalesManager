package com.trader.core.data.local.dao

import androidx.room.*
import com.trader.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class DebtAgingProjection(
    val lessThanWeekAmount: Double,
    val oneWeekToOneMonthAmount: Double,
    val oneMonthToThreeMonthsAmount: Double,
    val moreThanThreeMonthsAmount: Double,
    val lessThanWeekCount: Int,
    val oneWeekToOneMonthCount: Int,
    val oneMonthToThreeMonthsCount: Int,
    val moreThanThreeMonthsCount: Int
)

data class TodaySalesSummaryProjection(
    val todaySales: Double,
    val todayInvoiceCount: Int,
    val todayPaid: Double,
    val todayUnpaid: Double
)

data class TopDebtorCustomerProjection(
    val customerId: Long,
    val customerName: String,
    val totalDebt: Double
)

data class DailySalesProjection(
    val dayStartMillis: Long,
    val totalSales: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsByCustomer(customerId: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isPaid = 0 ORDER BY date ASC")
    fun getUnpaidTransactions(): Flow<List<TransactionEntity>>

    /** Returns unpaid transactions older than [olderThanMillis] timestamp — for debt reminder */
    @Query("SELECT * FROM transactions WHERE isPaid = 0 AND date <= :olderThanMillis ORDER BY date ASC")
    suspend fun getUnpaidOlderThan(olderThanMillis: Long): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeTransactionById(id: Long): Flow<TransactionEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    @Update suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isPaid = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isPaid = 0 AND customerId = :customerId")
    suspend fun getUnpaidAmountByCustomer(customerId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions
        WHERE isPaid = 0
          AND customerId = :customerId
          AND (date < :transactionDate OR (date = :transactionDate AND id < :transactionId))
        """
    )
    suspend fun getUnpaidAmountBeforeTransaction(
        customerId: Long,
        transactionDate: Long,
        transactionId: Long
    ): Double

    @Query(
        """
        SELECT
            COALESCE(SUM(amount), 0.0) AS todaySales,
            COUNT(*) AS todayInvoiceCount,
            COALESCE(SUM(CASE WHEN isPaid = 1 THEN amount ELSE 0.0 END), 0.0) AS todayPaid,
            COALESCE(SUM(CASE WHEN isPaid = 0 THEN amount ELSE 0.0 END), 0.0) AS todayUnpaid
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        """
    )
    fun observeTodaySalesSummary(
        startDate: Long,
        endDate: Long
    ): Flow<TodaySalesSummaryProjection>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isPaid = 0")
    fun observeTotalOutstandingDebt(): Flow<Double>

    @Query(
        """
        SELECT
            t.customerId AS customerId,
            c.name AS customerName,
            COALESCE(SUM(t.amount), 0.0) AS totalDebt
        FROM transactions t
        INNER JOIN customers c ON c.id = t.customerId
        WHERE t.isPaid = 0
        GROUP BY t.customerId, c.name
        ORDER BY totalDebt DESC
        LIMIT :limit
        """
    )
    fun observeTopDebtorCustomers(limit: Int): Flow<List<TopDebtorCustomerProjection>>

    @Query(
        """
        WITH days(dayIndex, dayStartMillis, dayEndMillis) AS (
            SELECT 0, :day0, :day1
            UNION ALL
            SELECT 1, :day1, :day2
            UNION ALL
            SELECT 2, :day2, :day3
            UNION ALL
            SELECT 3, :day3, :day4
            UNION ALL
            SELECT 4, :day4, :day5
            UNION ALL
            SELECT 5, :day5, :day6
            UNION ALL
            SELECT 6, :day6, :day7
        )
        SELECT
            days.dayStartMillis AS dayStartMillis,
            COALESCE(SUM(t.amount), 0.0) AS totalSales
        FROM days
        LEFT JOIN transactions t
            ON t.date >= days.dayStartMillis
            AND t.date < days.dayEndMillis
        GROUP BY days.dayIndex, days.dayStartMillis
        ORDER BY days.dayIndex ASC
        """
    )
    fun observeLastSevenDaysSales(
        day0: Long,
        day1: Long,
        day2: Long,
        day3: Long,
        day4: Long,
        day5: Long,
        day6: Long,
        day7: Long
    ): Flow<List<DailySalesProjection>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN date > :oneWeekAgo THEN amount ELSE 0.0 END), 0.0) AS lessThanWeekAmount,
            COALESCE(SUM(CASE WHEN date <= :oneWeekAgo AND date > :oneMonthAgo THEN amount ELSE 0.0 END), 0.0) AS oneWeekToOneMonthAmount,
            COALESCE(SUM(CASE WHEN date <= :oneMonthAgo AND date > :threeMonthsAgo THEN amount ELSE 0.0 END), 0.0) AS oneMonthToThreeMonthsAmount,
            COALESCE(SUM(CASE WHEN date <= :threeMonthsAgo THEN amount ELSE 0.0 END), 0.0) AS moreThanThreeMonthsAmount,
            COALESCE(SUM(CASE WHEN date > :oneWeekAgo THEN 1 ELSE 0 END), 0) AS lessThanWeekCount,
            COALESCE(SUM(CASE WHEN date <= :oneWeekAgo AND date > :oneMonthAgo THEN 1 ELSE 0 END), 0) AS oneWeekToOneMonthCount,
            COALESCE(SUM(CASE WHEN date <= :oneMonthAgo AND date > :threeMonthsAgo THEN 1 ELSE 0 END), 0) AS oneMonthToThreeMonthsCount,
            COALESCE(SUM(CASE WHEN date <= :threeMonthsAgo THEN 1 ELSE 0 END), 0) AS moreThanThreeMonthsCount
        FROM transactions
        WHERE isPaid = 0
        """
    )
    fun observeDebtAging(
        oneWeekAgo: Long,
        oneMonthAgo: Long,
        threeMonthsAgo: Long
    ): Flow<DebtAgingProjection>

    @Query("DELETE FROM transactions") suspend fun deleteAll()
    
    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: Long)
    
    @Query("SELECT id FROM transactions")
    suspend fun getAllIds(): List<Long>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE syncStatus != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED' ORDER BY date DESC")
    fun observeUnsynced(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET syncStatus = 'FAILED' WHERE id = :id")
    suspend fun markFailed(id: Long)

    /** عند حذف عميل → تُنقل عملياته للزبون الزائر (id=-1) بدل حذفها */
    @Query("UPDATE transactions SET customerId = -1 WHERE customerId = :customerId")
    suspend fun reassignToVisitor(customerId: Long)

    /**
     * ✅ Fix 3 (Self-Healing): يُصحح قيمة hasItems لجميع العمليات بناءً على وجود
     * أصناف فعلية في invoice_items. يُستدعى بعد كل sync لضمان تطابق البيانات
     * حتى مع البيانات القديمة في Firebase التي لم تحتوِ hasItems.
     */
    @Query("""
        UPDATE transactions
        SET hasItems = CASE
            WHEN id IN (SELECT DISTINCT transactionId FROM invoice_items) THEN 1
            ELSE 0
        END
    """)
    suspend fun recalculateHasItems()
}
