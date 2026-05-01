package com.trader.core.data.repository

import androidx.room.withTransaction
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ReturnRepositoryImpl(
    private val db: AppDatabase,
    private val stockRepo: StockRepository,
    private val transactionRepo: TransactionRepository,
    private val merchantId: String
) : ReturnRepository {

    private val dao = db.returnDao()

    // ════════════════════════════════════════════════════════════
    // الدالة الرئيسية — تعالج كل شيء في transaction واحدة
    // ════════════════════════════════════════════════════════════
    override suspend fun processReturn(
        returnInvoice: ReturnInvoice,
        items: List<ReturnItem>
    ): ReturnInvoice {

        db.withTransaction {
            // 1. التحقق من عدم تجاوز الكميات
            items.forEach { item ->
                val alreadyReturned = dao.totalReturnedForUnit(
                    returnInvoice.originalTransactionId, item.unitId
                )
                val maxReturnable = item.originalQuantity - alreadyReturned
                require(item.returnedQuantity <= maxReturnable) {
                    "الكمية المُرجَعة (${item.returnedQuantity}) تتجاوز الحد المسموح (${maxReturnable}) لـ ${item.productName}"
                }
            }

            // 2. حفظ فاتورة الإرجاع محلياً
            dao.insertReturnInvoice(returnInvoice.toEntity())
            dao.insertReturnItems(items.map { it.toEntity() })

            // 3. إعادة المخزون لكل صنف
            // Test Case 1: productId قد يكون لمنتج محذوف — returnStock يتعامل معه بشكل آمن
            items.forEach { item ->
                runCatching {
                    stockRepo.returnStock(
                        productId     = item.productId,
                        unitId        = item.unitId,
                        quantity      = item.returnedQuantity,
                        transactionId = returnInvoice.originalTransactionId,
                        productName   = item.productName,
                        unitLabel     = item.unitLabel
                    )
                } // إذا فشل (منتج محذوف) → لا يوقف الإرجاع المالي
            }

            // 4. تعديل مبلغ العملية الأصلية
            val originalTx = transactionRepo.getTransactionById(
                returnInvoice.originalTransactionId
            )
            if (originalTx != null) {
                val newAmount = (originalTx.amount - returnInvoice.totalRefund).coerceAtLeast(0.0)
                transactionRepo.updateTransaction(
                    originalTx.copy(amount = newAmount)
                )
            }
        }

        // 5. رفع لـ Firebase (offline-first — يفشل بصمت ويُزامن لاحقاً)
        // Test Case 2: إذا كان offline → PENDING في Room → WorkManager يرفعها لاحقاً
        runCatching { pushToFirebase(returnInvoice, items) }

        return returnInvoice
    }

    private suspend fun pushToFirebase(
        returnInvoice: ReturnInvoice,
        items: List<ReturnItem>
    ) {
        val rtdb = FirebaseDatabase.getInstance().reference
            .child("merchants")
            .child(merchantId)
            .child("return_invoices")
            .child(returnInvoice.id)

        val data = mapOf(
            "originalTransactionId" to returnInvoice.originalTransactionId,
            "returnType"            to returnInvoice.returnType.name,
            "totalRefund"           to returnInvoice.totalRefund,
            "note"                  to returnInvoice.note,
            "createdAt"             to returnInvoice.createdAt,
            "items"                 to items.associate { it.id to mapOf(
                "productId"       to it.productId,
                "productName"     to it.productName,
                "unitLabel"       to it.unitLabel,
                "returnedQty"     to it.returnedQuantity,
                "pricePerUnit"    to it.pricePerUnit,
                "totalRefund"     to it.totalRefund
            )}
        )
        rtdb.setValue(data).await()
        dao.markSynced(returnInvoice.id)
    }

    override fun getReturnsByTransaction(transactionId: Long): Flow<List<ReturnInvoice>> =
        dao.getReturnsByTransaction(transactionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getReturnItems(returnInvoiceId: String): List<ReturnItem> =
        dao.getReturnItems(returnInvoiceId).map { it.toDomain() }

    override fun getAllReturns(): Flow<List<ReturnInvoice>> =
        dao.getAllReturns(merchantId).map { list -> list.map { it.toDomain() } }

    // Test Case 3: Feature Flag من Firebase Realtime DB
    override suspend fun isPartialReturnEnabled(): Boolean = runCatching {
        val snap = FirebaseDatabase.getInstance().reference
            .child("activation_codes")
            .child(merchantId)
            .child("features")
            .child("is_partial_return_enabled")
            .get().await()
        snap.getValue(Boolean::class.java) ?: false
    }.getOrDefault(false) // إذا فشل (offline) → مجاني
}
