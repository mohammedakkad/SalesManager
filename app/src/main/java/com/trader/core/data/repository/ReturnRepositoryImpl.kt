package com.trader.core.data.repository

import androidx.room.withTransaction
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

class ReturnRepositoryImpl(
    private val db: AppDatabase,
    private val stockRepo: StockRepository,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val merchantId: String,
    private val activationRepo: ActivationRepository, // ✅ Added for Realtime Sync
    private val sync: FirebaseSyncService,
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ReturnRepository {

    private val dao = db.returnDao()
    private val uploadMutex = Mutex()

    init {
        startRealtimeSync()
    }

    // ✅ دالة التزامن اللحظي المضافة لتطبيق الـ SSOT
    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
                .distinctUntilChanged()
                .collectLatest { code ->
                    if (code.isBlank() || code != merchantId) return@collectLatest
                    sync.observeReturnInvoices(code).collect { remoteReturns ->
                        val remoteIds = remoteReturns.map { it.first.id }.toSet()

                        // 1. Upsert remote changes
                        remoteReturns.forEach { (invoice, items) ->
                            try {
                                dao.mergeRemoteReturn(
                                    invoice.copy(syncStatus = SyncStatus.SYNCED).toEntity(),
                                    items.map { it.toEntity() }
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                // Related data can arrive in a later snapshot; do not erase local writes.
                            }
                        }

                        // 2. Cleanup locally deleted remote records
                        val localIds = dao.getAllReturnIds()
                        localIds.forEach { localId ->
                            if (localId !in remoteIds) {
                                dao.deleteReturnInvoiceById(localId)
                            }
                        }
                    }
                }
        }
    }

    override suspend fun processReturn(
        returnInvoice: ReturnInvoice,
        items: List<ReturnItem>
    ): ReturnInvoice {

        require(items.isNotEmpty()) { "Return items are required" }
        require(items.map { it.id }.distinct().size == items.size) { "Duplicate return item IDs" }
        items.forEach { item ->
            item.validate().getOrThrow()
            require(item.returnInvoiceId == returnInvoice.id) { "Mismatched return invoice ID" }
            require(kotlin.math.abs(item.totalRefund - item.returnedQuantity * item.pricePerUnit) <= 0.000001) {
                "Refund does not match returned quantity and price"
            }
        }
        require(returnInvoice.totalRefund.isFinite() && returnInvoice.totalRefund >= 0 &&
            kotlin.math.abs(returnInvoice.totalRefund - items.sumOf { it.totalRefund }) <= 0.000001) {
            "Refund total does not match items"
        }
        val invoiceWithMerchant = returnInvoice.copy(merchantId = merchantId, syncStatus = SyncStatus.PENDING)
        val persisted = db.withTransaction {
            val existing = dao.getById(returnInvoice.id)
            if (existing != null) {
                require(existing.toDomain().copy(syncStatus = SyncStatus.PENDING) == invoiceWithMerchant &&
                    dao.getReturnItems(existing.id).sortedBy { it.id } == items.map { it.toEntity() }.sortedBy { it.id }) {
                    "Return ID already exists with a different payload"
                }
                return@withTransaction existing.toDomain()
            }
            val originalTx = requireNotNull(transactionRepo.getTransactionById(returnInvoice.originalTransactionId)) {
                "Original transaction not found"
            }
            val invoiceItems = invoiceItemRepo.getItemsForTransactionOnce(originalTx.id)
            items.groupBy { it.unitId }.forEach { (unitId, requested) ->
                val sold = invoiceItems.filter { it.unitId == unitId }
                require(sold.isNotEmpty() && requested.all { item -> sold.any { it.productId == item.productId } }) {
                    "Returned unit is not part of the original invoice"
                }
                val remaining = sold.sumOf { it.quantity } - dao.totalReturnedForUnit(originalTx.id, unitId)
                val quantity = requested.sumOf { it.returnedQuantity }
                // Permit only arithmetic representation error, not a business quantity tolerance.
                val roundingError = 4 * Math.ulp(maxOf(kotlin.math.abs(quantity), kotlin.math.abs(remaining)))
                require(quantity <= remaining || quantity - remaining <= roundingError) { "Returned quantity exceeds remaining sold stock" }
            }
            require(returnInvoice.totalRefund <= originalTx.amount + 0.000001) { "Refund exceeds remaining sale amount" }
            dao.insertReturnWithItems(invoiceWithMerchant.toEntity(), items.map { it.toEntity() })
            items.forEach { item ->
                // Deleted products cannot be restocked. Actual write failures must roll back the return.
                if (db.productDao().getProductIdForUnit(item.unitId) != null) {
                    stockRepo.returnStock(item.productId, item.unitId, item.returnedQuantity,
                        originalTx.id, item.productName, item.unitLabel)
                }
            }
            transactionRepo.updateTransaction(originalTx.copy(
                amount = (originalTx.amount - returnInvoice.totalRefund).coerceAtLeast(0.0),
                returnStatus = computeReturnStatus(originalTx.id, invoiceItems)
            ))
            invoiceWithMerchant
        }
        // Retry only the durable upload; never replay stock/cash effects.
        syncScope.launch { syncPendingReturns() }
        return persisted
    }

    override suspend fun syncPendingReturns() = uploadMutex.withLock {
        if (merchantId.isBlank() || activationRepo.getMerchantCode() != merchantId) return@withLock
        dao.getPendingReturns(merchantId).forEach { entity ->
            if (activationRepo.getMerchantCode() != merchantId) return@withLock
            try {
                sync.pushReturnInvoice(merchantId, entity.toDomain(), dao.getReturnItems(entity.id).map { it.toDomain() })
                dao.markSynced(entity.id)
            } catch (e: TimeoutCancellationException) {
                dao.markFailed(entity.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dao.markFailed(entity.id)
            }
        }
    }

    // ── ملخص الإرجاع لعرض الحالة في الـ UI ──────────────────────────
    override suspend fun getReturnSummary(
        transactionId: Long,
        invoiceItems: List<InvoiceItem>
    ): ReturnSummary {
        if (invoiceItems.isEmpty()) return ReturnSummary.NONE

        val returnedByUnit = dao.getReturnedByUnit(transactionId)
        .associate {
            it.unitId to it.total
        }

        if (returnedByUnit.isEmpty()) return ReturnSummary.NONE

        // ✅ الإصلاح المالي: حساب المبلغ المُسترد بضرب الكمية في سعر الوحدة
        val totalRefunded = invoiceItems.sumOf { item ->
            val qty = returnedByUnit[item.unitId] ?: 0.0
            qty * item.pricePerUnit
        }

        // ✅ الحالة: مكتمل إذا كل صنف أُرجع بكامل كميته
        val allFullyReturned = invoiceItems.all {
            item ->
            val returned = returnedByUnit[item.unitId] ?: 0.0
            returned >= item.quantity
        }

        val status = when {
            allFullyReturned -> TransactionReturnStatus.FULLY_RETURNED
            else -> TransactionReturnStatus.PARTIALLY_RETURNED
        }

        return ReturnSummary(
            returnStatus = status,
            totalRefunded = totalRefunded,
            returnedByUnit = returnedByUnit
        )
    }

    override fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoice>> =
    dao.getReturnsByTransaction(transactionId).map {
        it.map {
            e -> e.toDomain()
        }
    }

    override suspend fun getReturnItems(returnInvoiceId: String): List<ReturnItem> =
    dao.getReturnItems(returnInvoiceId).map {
        it.toDomain()
    }

    /** ✅ الكمية المُرجَعة فعلاً لوحدة معينة — يستخدمها ReturnViewModel */
    override suspend fun getAlreadyReturnedQty(transactionId: Long, unitId: String): Double =
    dao.totalReturnedForUnit(transactionId, unitId)

    override fun getAllReturns(): Flow<List<ReturnInvoice>> =
    dao.getAllReturns(merchantId).map {
        it.map {
            e -> e.toDomain()
        }
    }

    // ── حساب returnStatus الجديد بعد الإرجاع ─────────────────────
    private suspend fun computeReturnStatus(
        transactionId: Long,
        invoiceItems: List<InvoiceItem>
    ): TransactionReturnStatus {
        if (invoiceItems.isEmpty()) return TransactionReturnStatus.PARTIALLY_RETURNED

        val returnedByUnit = dao.getReturnedByUnit(transactionId).associate {
            it.unitId to it.total
        }

        val allFullyReturned = invoiceItems.all {
            item ->
            (returnedByUnit[item.unitId] ?: 0.0) >= item.quantity
        }
        val anyReturned = returnedByUnit.any {
            it.value > 0
        }

        return when {
            allFullyReturned -> TransactionReturnStatus.FULLY_RETURNED
            anyReturned -> TransactionReturnStatus.PARTIALLY_RETURNED
            else -> TransactionReturnStatus.NONE
        }
    }

}
