package com.trader.core.data.repository

import androidx.room.withTransaction
import com.google.firebase.database.FirebaseDatabase
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch

class ReturnRepositoryImpl(
    private val db: AppDatabase,
    private val stockRepo: StockRepository,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val merchantId: String,
    private val activationRepo: ActivationRepository, // ✅ Added for Realtime Sync
    private val sync: FirebaseSyncService             // ✅ Added for Realtime Sync
) : ReturnRepository {

    private val dao = db.returnDao()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    // ✅ دالة التزامن اللحظي المضافة لتطبيق الـ SSOT
    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
                .filter { it.isNotEmpty() } // Wait for valid code
                .distinctUntilChanged()
                .collectLatest { code ->
                    sync.observeReturnInvoices(code).collect { remoteReturns ->
                        val remoteIds = remoteReturns.map { it.first.id }.toSet()

                        // 1. Upsert remote changes
                        remoteReturns.forEach { (invoice, items) ->
                            try {
                                dao.insertReturnWithItems(
                                    invoice.copy(syncStatus = SyncStatus.SYNCED).toEntity(),
                                    items.map { it.toEntity() }
                                )
                            } catch (e: Exception) {
                                // Fallback silently on constraint issues if related data hasn't synced yet
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

        // ── التحقق من صحة كل صنف قبل البدء ─────────────────────────
        items.forEach {
            item ->
            item.validate().getOrThrow()
        }

        db.withTransaction {

            // ── 1. التحقق من الكميات المتبقية (منع Double Return) ────
            items.forEach {
                item ->
                val alreadyReturned = dao.totalReturnedForUnit(
                    returnInvoice.originalTransactionId, item.unitId
                )
                val maxReturnable = item.originalQuantity - alreadyReturned
                require(item.returnedQuantity <= maxReturnable) {
                    "الكمية المُرجَعة (${item.returnedQuantity}) تتجاوز الحد (${maxReturnable}) لـ ${item.productName}"
                }
            }

            // ── 2. حفظ فاتورة الإرجاع ───────────────────────────────
            val invoiceWithMerchant = returnInvoice.copy(merchantId = merchantId)
            dao.insertReturnWithItems(
                invoiceWithMerchant.toEntity(), 
                items.map { it.toEntity() }
            )

            // ── 3. إعادة المخزون (يفشل بصمت إذا الصنف محذوف) ────────
            items.forEach {
                item ->
                runCatching {
                    stockRepo.returnStock(
                        productId = item.productId,
                        unitId = item.unitId,
                        quantity = item.returnedQuantity,
                        transactionId = returnInvoice.originalTransactionId,
                        productName = item.productName,
                        unitLabel = item.unitLabel
                    )
                }
            }

            // ── 4. تحديث مبلغ + returnStatus العملية الأصلية ────────
            val originalTx = transactionRepo.getTransactionById(
                returnInvoice.originalTransactionId
            )
            if (originalTx != null) {
                val newAmount = (originalTx.amount - returnInvoice.totalRefund).coerceAtLeast(0.0)

                // ✅ جلب أصناف الفاتورة لحساب returnStatus بدقة
                val invoiceItems = invoiceItemRepo.getItemsForTransactionOnce(
                    returnInvoice.originalTransactionId
                )
                val newStatus = computeReturnStatus(
                    transactionId = originalTx.id,
                    invoiceItems = invoiceItems
                )

                transactionRepo.updateTransaction(
                    originalTx.copy(
                        amount = newAmount,
                        // ✅ نحفظ originalAmount مرة واحدة فقط (في أول إرجاع)
                        originalAmount = if (originalTx.originalAmount == originalTx.amount)
                            originalTx.amount else originalTx.originalAmount,
                        returnStatus = newStatus
                    )
                )
            }
        }

        // ✅ Firebase في الخلفية — لا ينتظر (لا يعلق UI عند ضعف الاتصال)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                pushToFirebase(returnInvoice, items)
            }
        }

        return returnInvoice.copy(merchantId = merchantId)
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

    // ── Firebase — مع timeout لمنع التعليق أثناء ضعف الاتصال ─────
    private suspend fun pushToFirebase(returnInvoice: ReturnInvoice, items: List<ReturnItem>) {
        withTimeout(8_000) {
            val ref = FirebaseDatabase.getInstance().reference
                .child("merchants").child(merchantId)
                .child("return_invoices").child(returnInvoice.id)

            ref.setValue(mapOf(
                "id"                    to returnInvoice.id,
                "merchantId"            to merchantId,
                "originalTransactionId" to returnInvoice.originalTransactionId,
                "returnType"            to returnInvoice.returnType.name,
                "totalRefund"           to returnInvoice.totalRefund,
                "note"                  to returnInvoice.note,
                "createdAt"             to returnInvoice.createdAt,
                "items" to items.associate { item ->
                    item.id to mapOf(
                        "id"               to item.id,
                        "productId"        to item.productId,
                        "productName"      to item.productName,
                        "unitId"           to item.unitId,
                        "unitLabel"        to item.unitLabel,
                        "originalQuantity" to item.originalQuantity,
                        "returnedQty"      to item.returnedQuantity,
                        "pricePerUnit"     to item.pricePerUnit,
                        "costPricePerUnit" to item.costPricePerUnit,
                        "totalRefund"      to item.totalRefund,
                        "lostProfit"       to item.lostProfit
                    )
                }
            )).await()
            dao.markSynced(returnInvoice.id)
        }
    }

}