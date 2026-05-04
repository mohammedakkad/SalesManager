package com.trader.core.data.repository

import androidx.room.withTransaction
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class ReturnRepositoryImpl(
    private val db: AppDatabase,
    private val stockRepo: StockRepository,
    private val transactionRepo: TransactionRepository,
    private val invoiceItemRepo: InvoiceItemRepository, // ✅ مطلوب لـ computeReturnStatus
    private val merchantId: String
) : ReturnRepository {

    private val dao = db.returnDao()

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
            dao.insertReturnInvoice(invoiceWithMerchant.toEntity())
            dao.insertReturnItems(items.map {
                it.toEntity()
            })

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
        // فشل الرفع لا يؤثر على الحفظ المحلي — المزامنة تحدث عند إعادة الاتصال
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

        val totalRefunded = returnedByUnit.values.sum()

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

    override suspend fun isPartialReturnEnabled(): Boolean = runCatching {
        withTimeout(3_000) {
            val snap = FirebaseDatabase.getInstance().reference
            .child("activation_codes").child(merchantId)
            .child("features").child("is_partial_return_enabled")
            .get().await()
            snap.getValue(Boolean::class.java) ?: false
        }
    }.getOrDefault(false)

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
            // ✅ 8 ثوانٍ حد أقصى — لا يعلق إلى الأبد
            val ref = FirebaseDatabase.getInstance().reference
            .child("merchants").child(merchantId)
            .child("return_invoices").child(returnInvoice.id)

            ref.setValue(mapOf(
                "originalTransactionId" to returnInvoice.originalTransactionId,
                "returnType" to returnInvoice.returnType.name,
                "totalRefund" to returnInvoice.totalRefund,
                "note" to returnInvoice.note,
                "createdAt" to returnInvoice.createdAt,
                "items" to items.associate {
                    it.id to mapOf(
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "unitLabel" to it.unitLabel,
                        "returnedQty" to it.returnedQuantity,
                        "pricePerUnit" to it.pricePerUnit,
                        "totalRefund" to it.totalRefund
                    )}
            )).await()
            dao.markSynced(returnInvoice.id)
        }
    }

}