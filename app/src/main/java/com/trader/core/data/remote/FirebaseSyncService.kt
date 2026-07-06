package com.trader.core.data.remote

import androidx.work.await
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.Transaction as AppTransaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.SubscriptionStatus
import com.trader.core.domain.model.ReturnInvoice
import com.trader.core.domain.model.ReturnItem
import com.trader.core.domain.model.ReturnType
import com.trader.core.domain.model.TransactionReturnStatus
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.EmployeeRole
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.SyncStatus
import java.util.UUID

class FirebaseSyncService {
    private val db = FirebaseDatabase.getInstance()

    private fun Any?.asLong(): Long? = when (this) {
        is Long -> this; is Int -> toLong(); is Double -> toLong(); is String -> toLongOrNull(); else -> null
    }

    private fun Any?.asDouble(): Double? = when (this) {
        is Double -> this; is Long -> toDouble(); is Int -> toDouble(); is String -> toDoubleOrNull(); else -> null
    }

    suspend fun validateCodeDetailed(code: String): ValidationResult {
        return try {
            val snap = withTimeoutOrNull(8_000) {
                db.reference.child(PATH_ACTIVATION_CODES).child(code).get().await()
            } ?: return ValidationResult.NetworkError

            if (!snap.exists()) return ValidationResult.NotFound

            val status = snap.child(KEY_STATUS).getValue(String::class.java)?.uppercase()
                ?: return ValidationResult.NotFound

            when (status) {
                "ACTIVE" -> ValidationResult.Active
                "DISABLED" -> ValidationResult.Disabled
                "EXPIRED" -> ValidationResult.Expired
                else -> ValidationResult.NotFound
            }
        } catch (e: Exception) {
            ValidationResult.NetworkError
        }
    }

    fun observeSubscriptionRequestStatus(merchantCode: String): Flow<SubscriptionStatus> = callbackFlow {
        val nodeReference = db.getReference(PATH_SUBSCRIPTION_REQUESTS).child(merchantCode)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latestRequest = snapshot.children.lastOrNull()
                val statusString = latestRequest?.child(KEY_STATUS)?.getValue(String::class.java)

                val status = runCatching {
                    SubscriptionStatus.valueOf(statusString ?: "")
                }.getOrDefault(SubscriptionStatus.PENDING)

                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        nodeReference.addValueEventListener(listener)
        awaitClose {
            nodeReference.removeEventListener(listener)
        }
    }

    fun observeCurrentSession(merchantCode: String, sessionId: String): Flow<Boolean> = callbackFlow {
        val ref = db.reference
            .child("merchants")
            .child(merchantCode)
            .child("sessions")
            .child(sessionId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        runCatching { ref.addValueEventListener(listener) }
            .onFailure { close(it) }

        awaitClose {
            runCatching { ref.removeEventListener(listener) }
        }
    }

    suspend fun fetchMerchantActivationData(merchantCode: String): DataSnapshot {
        return db.getReference(PATH_ACTIVATION_CODES).child(merchantCode).get().await()
    }

    suspend fun validateCode(code: String): Boolean =
        validateCodeDetailed(code) == ValidationResult.Active

    suspend fun getCodeStatus(code: String): String? {
        return try {
            val snap = withTimeoutOrNull(8_000) {
                db.reference.child(PATH_ACTIVATION_CODES).child(code).get().await()
            } ?: return null

            if (!snap.exists()) return "DELETED"

            val status = snap.child(KEY_STATUS).getValue(String::class.java)?.uppercase()
                ?: return "DELETED"

            when (status) {
                "ACTIVE" -> "ACTIVE"
                "DISABLED" -> "DISABLED"
                "EXPIRED" -> "EXPIRED"
                else -> "DELETED"
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllData(merchantCode: String): MerchantData {
        val root = db.reference.child("merchants").child(merchantCode)

        val customers = try {
            root.child("customers").get().await().children.mapNotNull { snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                Customer(
                    id = m["id"].asLong() ?: snap.key?.toLongOrNull() ?: return@mapNotNull null,
                    name = m["name"] as? String ?: return@mapNotNull null,
                    phone = m["phone"] as? String ?: "",
                    createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val paymentMethods = try {
            root.child("payment_methods").get().await().children.mapNotNull { snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                PaymentMethod(
                    id = m["id"].asLong() ?: snap.key?.toLongOrNull() ?: return@mapNotNull null,
                    name = m["name"] as? String ?: return@mapNotNull null,
                    type = runCatching {
                        PaymentType.valueOf(m["type"] as? String ?: "")
                    }.getOrDefault(PaymentType.OTHER)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val transactions = try {
            root.child("transactions").get().await().children.mapNotNull { snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                AppTransaction(
                    id = m["id"].asLong() ?: snap.key?.toLongOrNull() ?: return@mapNotNull null,
                    customerId = m["customerId"].asLong() ?: return@mapNotNull null,
                    amount = m["amount"].asDouble() ?: return@mapNotNull null,
                    originalAmount = m["originalAmount"].asDouble() ?: m["amount"].asDouble() ?: 0.0,
                    returnStatus = runCatching {
                        TransactionReturnStatus.valueOf(m["returnStatus"] as? String ?: "NONE")
                    }.getOrDefault(TransactionReturnStatus.NONE),
                    isPaid = m["isPaid"] as? Boolean ?: false,
                    paymentMethodId = m["paymentMethodId"].asLong(),
                    note = m["note"] as? String ?: "",
                    date = m["date"].asLong() ?: System.currentTimeMillis(),
                    paidAt = m["paidAt"].asLong(),
                    hasItems = m["hasItems"] as? Boolean ?: false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val invoiceItems = try {
            root.child("invoice_items").get().await().children.mapNotNull { snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                InvoiceItem(
                    id = m["id"] as? String ?: return@mapNotNull null,
                    transactionId = m["transactionId"].asLong() ?: return@mapNotNull null,
                    productId = m["productId"] as? String ?: "",
                    productName = m["productName"] as? String ?: "",
                    unitId = m["unitId"] as? String ?: "",
                    unitLabel = m["unitLabel"] as? String ?: "",
                    quantity = m["quantity"].asDouble() ?: 0.0,
                    pricePerUnit = m["pricePerUnit"].asDouble() ?: 0.0,
                    totalPrice = m["totalPrice"].asDouble() ?: 0.0,
                    merchantId = merchantCode
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val returns = try {
            root.child("return_invoices").get().await().children.mapNotNull { snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                val invoiceId = m["id"] as? String ?: snap.key ?: return@mapNotNull null
                val originalTxId = m["originalTransactionId"].asLong() ?: m["transactionId"].asLong() ?: return@mapNotNull null

                val invoice = ReturnInvoice(
                    id = invoiceId,
                    originalTransactionId = originalTxId,
                    merchantId = merchantCode,
                    returnType = runCatching {
                        ReturnType.valueOf(m["returnType"] as? String ?: "")
                    }.getOrDefault(ReturnType.PARTIAL),
                    totalRefund = m["totalRefund"].asDouble() ?: 0.0,
                    note = m["note"] as? String ?: "",
                    createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                )

                val itemsRaw = m["items"]
                val itemsIterable = when (itemsRaw) {
                    is Map<*, *> -> itemsRaw.values
                    is List<*> -> itemsRaw.filterNotNull()
                    else -> emptyList<Any>()
                }

                val items = itemsIterable.mapNotNull { itemRaw ->
                    val im = itemRaw as? Map<*, *> ?: return@mapNotNull null
                    val pId = im["productId"] as? String ?: ""

                    val matchedInvoiceItem = invoiceItems.firstOrNull {
                        it.transactionId == originalTxId && it.productId == pId
                    }

                    val fallbackUnitId = matchedInvoiceItem?.unitId ?: ""
                    val finalUnitId = im["unitId"] as? String ?: fallbackUnitId

                    val fallbackOrigQty = matchedInvoiceItem?.quantity ?: 0.0
                    val finalOrigQty = im["originalQuantity"].asDouble() ?: fallbackOrigQty

                    if (finalUnitId.isEmpty()) return@mapNotNull null

                    ReturnItem(
                        id = im["id"] as? String ?: UUID.randomUUID().toString(),
                        returnInvoiceId = invoiceId,
                        productId = pId,
                        productName = im["productName"] as? String ?: matchedInvoiceItem?.productName ?: "",
                        unitId = finalUnitId,
                        unitLabel = im["unitLabel"] as? String ?: matchedInvoiceItem?.unitLabel ?: "",
                        originalQuantity = finalOrigQty,
                        returnedQuantity = im["returnedQty"].asDouble() ?: im["returnedQuantity"].asDouble() ?: 0.0,
                        costPricePerUnit = im["costPricePerUnit"].asDouble() ?: 0.0,
                        lostProfit = im["lostProfit"].asDouble() ?: 0.0,
                        pricePerUnit = im["pricePerUnit"].asDouble() ?: matchedInvoiceItem?.pricePerUnit ?: 0.0,
                        totalRefund = im["totalRefund"].asDouble() ?: 0.0
                    )
                }
                Pair(invoice, items)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val cashBoxes = try {
            root.child("cash_boxes").get().await().children.mapNotNull { snap ->
                snap.toCashBox(merchantCode)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return MerchantData(customers, transactions, paymentMethods, returns, invoiceItems, cashBoxes)
    }

    private fun DataSnapshot.toCashBox(merchantCode: String): CashBox? {
        val m = value as? Map<*, *> ?: return null
        val paymentMethodId = m["paymentMethodId"].asLong() ?: key?.toLongOrNull() ?: return null
        return CashBox(
            id = m["id"] as? String ?: key ?: return null,
            paymentMethodId = paymentMethodId,
            paymentMethodName = m["paymentMethodName"] as? String ?: "",
            currentBalance = m["currentBalance"].asDouble() ?: 0.0,
            initialBalance = m["initialBalance"].asDouble() ?: 0.0,
            initialBalanceSetAt = m["initialBalanceSetAt"].asLong(),
            merchantId = merchantCode,
            updatedAt = m["updatedAt"].asLong() ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }

    fun observeCashBoxes(merchantCode: String): Flow<List<CashBox>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("cash_boxes")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    runCatching { child.toCashBox(merchantCode) }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    /** قراءة صندوق واحد — تُستخدم قبل الإنشاء التلقائي حتى لا نمسح رصيداً موجوداً عن بُعد */
    suspend fun fetchCashBox(merchantCode: String, boxId: String): CashBox? {
        val snap = db.reference.child("merchants").child(merchantCode).child("cash_boxes")
            .child(boxId).get().await()
        return snap.toCashBox(merchantCode)
    }

    suspend fun pushCashBox(merchantCode: String, box: CashBox) {
        db.reference.child("merchants").child(merchantCode).child("cash_boxes")
            .child(box.id)
            .setValue(
                mapOf(
                    "id" to box.id,
                    "paymentMethodId" to box.paymentMethodId,
                    "paymentMethodName" to box.paymentMethodName,
                    "currentBalance" to box.currentBalance,
                    "initialBalance" to box.initialBalance,
                    "initialBalanceSetAt" to box.initialBalanceSetAt,
                    "updatedAt" to box.updatedAt
                )
            ).await()
    }

    suspend fun deleteCashBox(merchantCode: String, boxId: String) {
        db.reference.child("merchants").child(merchantCode).child("cash_boxes")
            .child(boxId).removeValue().await()
    }

    fun observeCustomers(merchantCode: String): Flow<List<Customer>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("customers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        Customer(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull() ?: return@mapNotNull null,
                            name = m["name"] as? String ?: return@mapNotNull null,
                            phone = m["phone"] as? String ?: "",
                            createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                        )
                    }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeTransactions(merchantCode: String): Flow<List<AppTransaction>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("transactions")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        AppTransaction(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull() ?: return@mapNotNull null,
                            customerId = m["customerId"].asLong() ?: return@mapNotNull null,
                            amount = m["amount"].asDouble() ?: return@mapNotNull null,
                            originalAmount = m["originalAmount"].asDouble() ?: m["amount"].asDouble() ?: 0.0,
                            returnStatus = runCatching {
                                TransactionReturnStatus.valueOf(m["returnStatus"] as? String ?: "NONE")
                            }.getOrDefault(TransactionReturnStatus.NONE),
                            isPaid = m["isPaid"] as? Boolean ?: false,
                            paymentMethodId = m["paymentMethodId"].asLong(),
                            note = m["note"] as? String ?: "",
                            date = m["date"].asLong() ?: System.currentTimeMillis(),
                            paidAt = m["paidAt"].asLong(),
                            hasItems = m["hasItems"] as? Boolean ?: false
                        )
                    }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observePaymentMethods(merchantCode: String): Flow<List<PaymentMethod>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("payment_methods")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        PaymentMethod(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull() ?: return@mapNotNull null,
                            name = m["name"] as? String ?: return@mapNotNull null,
                            type = runCatching {
                                PaymentType.valueOf(m["type"] as? String ?: "")
                            }.getOrDefault(PaymentType.OTHER)
                        )
                    }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeInvoiceItems(merchantCode: String): Flow<List<InvoiceItem>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("invoice_items")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        InvoiceItem(
                            id = m["id"] as? String ?: return@mapNotNull null,
                            transactionId = m["transactionId"].asLong() ?: return@mapNotNull null,
                            productId = m["productId"] as? String ?: return@mapNotNull null,
                            productName = m["productName"] as? String ?: "",
                            unitId = m["unitId"] as? String ?: return@mapNotNull null,
                            unitLabel = m["unitLabel"] as? String ?: "",
                            quantity = m["quantity"].asDouble() ?: return@mapNotNull null,
                            pricePerUnit = m["pricePerUnit"].asDouble() ?: 0.0,
                            totalPrice = m["totalPrice"].asDouble() ?: 0.0,
                            merchantId = merchantCode
                        )
                    }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    fun observeReturnInvoices(merchantCode: String): Flow<List<Pair<ReturnInvoice, List<ReturnItem>>>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("return_invoices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                launch {
                    val invoiceItemsSnap = runCatching {
                        db.reference.child("merchants").child(merchantCode).child("invoice_items").get().await()
                    }.getOrNull()

                    val invoiceItems = invoiceItemsSnap?.children?.mapNotNull { itemSnap ->
                        val m = itemSnap.value as? Map<*, *> ?: return@mapNotNull null
                        InvoiceItem(
                            id = m["id"] as? String ?: return@mapNotNull null,
                            transactionId = m["transactionId"].asLong() ?: return@mapNotNull null,
                            productId = m["productId"] as? String ?: "",
                            productName = m["productName"] as? String ?: "",
                            unitId = m["unitId"] as? String ?: "",
                            unitLabel = m["unitLabel"] as? String ?: "",
                            quantity = m["quantity"].asDouble() ?: 0.0,
                            pricePerUnit = m["pricePerUnit"].asDouble() ?: 0.0,
                            totalPrice = m["totalPrice"].asDouble() ?: 0.0,
                            merchantId = merchantCode
                        )
                    } ?: emptyList()

                    val returns = snap.children.mapNotNull { child ->
                        val m = child.value as? Map<*, *> ?: return@mapNotNull null
                        val invoiceId = m["id"] as? String ?: child.key ?: return@mapNotNull null
                        val originalTxId = m["originalTransactionId"].asLong() ?: m["transactionId"].asLong() ?: return@mapNotNull null

                        val invoice = ReturnInvoice(
                            id = invoiceId,
                            originalTransactionId = originalTxId,
                            merchantId = merchantCode,
                            returnType = runCatching { ReturnType.valueOf(m["returnType"] as? String ?: "") }.getOrDefault(ReturnType.PARTIAL),
                            totalRefund = m["totalRefund"].asDouble() ?: 0.0,
                            note = m["note"] as? String ?: "",
                            createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                        )

                        val itemsRaw = m["items"]
                        val itemsIterable = when (itemsRaw) {
                            is Map<*, *> -> itemsRaw.values
                            is List<*> -> itemsRaw.filterNotNull()
                            else -> emptyList<Any>()
                        }

                        val items = itemsIterable.mapNotNull { itemRaw ->
                            val im = itemRaw as? Map<*, *> ?: return@mapNotNull null
                            val pId = im["productId"] as? String ?: ""

                            val matchedInvoiceItem = invoiceItems.firstOrNull { it.transactionId == originalTxId && it.productId == pId }
                            val finalUnitId = im["unitId"] as? String ?: matchedInvoiceItem?.unitId ?: ""
                            val finalOrigQty = im["originalQuantity"].asDouble() ?: matchedInvoiceItem?.quantity ?: 0.0

                            if (finalUnitId.isEmpty()) return@mapNotNull null

                            ReturnItem(
                                id = im["id"] as? String ?: UUID.randomUUID().toString(),
                                returnInvoiceId = invoiceId,
                                productId = pId,
                                productName = im["productName"] as? String ?: matchedInvoiceItem?.productName ?: "",
                                unitId = finalUnitId,
                                unitLabel = im["unitLabel"] as? String ?: matchedInvoiceItem?.unitLabel ?: "",
                                originalQuantity = finalOrigQty,
                                returnedQuantity = im["returnedQty"].asDouble() ?: im["returnedQuantity"].asDouble() ?: 0.0,
                                costPricePerUnit = im["costPricePerUnit"].asDouble() ?: 0.0,
                                lostProfit = im["lostProfit"].asDouble() ?: 0.0,
                                pricePerUnit = im["pricePerUnit"].asDouble() ?: matchedInvoiceItem?.pricePerUnit ?: 0.0,
                                totalRefund = im["totalRefund"].asDouble() ?: 0.0
                            )
                        }
                        Pair(invoice, items)
                    }
                    trySend(returns)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun pushCustomer(merchantCode: String, c: Customer) {
        db.reference.child("merchants").child(merchantCode).child("customers")
            .child(c.id.toString())
            .setValue(mapOf("id" to c.id, "name" to c.name, "phone" to c.phone, "createdAt" to c.createdAt))
    }

    fun deleteCustomer(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(id.toString()).removeValue()
    }

    suspend fun pushTransaction(merchantCode: String, t: AppTransaction) {
        db.reference.child("merchants").child(merchantCode).child("transactions")
            .child(t.id.toString())
            .setValue(
                mapOf(
                    "id" to t.id, "customerId" to t.customerId, "amount" to t.amount,
                    "originalAmount" to t.originalAmount, "returnStatus" to t.returnStatus.name,
                    "isPaid" to t.isPaid, "paymentMethodId" to t.paymentMethodId,
                    "note" to t.note, "date" to t.date, "paidAt" to t.paidAt,
                    "hasItems" to t.hasItems
                )
            ).await()
    }

    suspend fun pushReturnInvoice(merchantCode: String, returnInvoice: ReturnInvoice, items: List<ReturnItem>) {
        withTimeoutOrNull(8_000) {
            val ref = db.reference.child("merchants").child(merchantCode).child("return_invoices").child(returnInvoice.id)
            ref.setValue(mapOf(
                "id" to returnInvoice.id,
                "originalTransactionId" to returnInvoice.originalTransactionId,
                "returnType" to returnInvoice.returnType.name,
                "totalRefund" to returnInvoice.totalRefund,
                "note" to returnInvoice.note,
                "createdAt" to returnInvoice.createdAt,
                "merchantId" to merchantCode,
                "items" to items.associate {
                    it.id to mapOf(
                        "id" to it.id,
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "unitId" to it.unitId,
                        "unitLabel" to it.unitLabel,
                        "originalQuantity" to it.originalQuantity,
                        "returnedQty" to it.returnedQuantity,
                        "pricePerUnit" to it.pricePerUnit,
                        "costPricePerUnit" to it.costPricePerUnit,
                        "totalRefund" to it.totalRefund
                    )
                }
            )).await()
        }
    }

    suspend fun deleteTransaction(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("transactions").child(id.toString()).removeValue().await()
    }

    fun pushPaymentMethod(merchantCode: String, m: PaymentMethod) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods")
            .child(m.id.toString()).setValue(mapOf("id" to m.id, "name" to m.name, "type" to m.type.name))
    }

    fun deletePaymentMethod(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods").child(id.toString()).removeValue()
    }

    suspend fun pushInvoiceItems(merchantCode: String, items: List<InvoiceItem>) {
        val ref = db.reference.child("merchants").child(merchantCode).child("invoice_items")
        items.forEach { item ->
            ref.child(item.id).setValue(
                mapOf(
                    "id" to item.id,
                    "transactionId" to item.transactionId,
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "unitId" to item.unitId,
                    "unitLabel" to item.unitLabel,
                    "quantity" to item.quantity,
                    "pricePerUnit" to item.pricePerUnit,
                    "totalPrice" to item.totalPrice
                )
            ).await()
        }
    }

    suspend fun deleteInvoiceItemsForTransaction(merchantCode: String, transactionId: Long) {
        val ref = db.reference.child("merchants").child(merchantCode).child("invoice_items")
        val snap = ref.orderByChild("transactionId").equalTo(transactionId.toDouble()).get().await()
        snap.children.forEach {
            it.ref.removeValue().await()
        }
    }

    suspend fun revokeSession(merchantCode: String, sessionId: String) {
        db.reference
            .child("merchants")
            .child(merchantCode)
            .child("sessions")
            .child(sessionId)
            .removeValue()
            .await()
    }

    fun observeEmployees(merchantCode: String): Flow<List<Employee>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("employees")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull { child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        Employee(
                            id = m["id"] as? String ?: child.key ?: return@mapNotNull null,
                            merchantId = merchantCode,
                            name = m["name"] as? String ?: return@mapNotNull null,
                            pinCode = m["pinCode"] as? String ?: return@mapNotNull null,
                            role = runCatching {
                                EmployeeRole.valueOf(m["role"] as? String ?: "")
                            }.getOrDefault(EmployeeRole.CASHIER),
                            createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                        )
                    }.getOrNull()
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun pushEmployee(merchantCode: String, e: Employee) {
        runCatching {
            db.reference.child("merchants").child(merchantCode).child("employees")
                .child(e.id)
                .setValue(
                    mapOf(
                        "id" to e.id,
                        "name" to e.name,
                        "pinCode" to e.pinCode,
                        "role" to e.role.name,
                        "createdAt" to e.createdAt
                    )
                ).await()
        }
    }

    suspend fun deleteEmployee(merchantCode: String, employeeId: String) {
        runCatching {
            db.reference.child("merchants").child(merchantCode).child("employees")
                .child(employeeId).removeValue().await()
        }
    }

    companion object {
        private const val PATH_SUBSCRIPTION_REQUESTS = "subscription_requests"
        private const val PATH_ACTIVATION_CODES = "activation_codes"
        private const val KEY_STATUS = "status"
    }
}

data class MerchantData(
    val customers: List<Customer>,
    val transactions: List<AppTransaction>,
    val paymentMethods: List<PaymentMethod>,
    val returns: List<Pair<ReturnInvoice, List<ReturnItem>>> = emptyList(),
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val cashBoxes: List<CashBox> = emptyList()
)

sealed class ValidationResult {
    object Active : ValidationResult()
    object Disabled : ValidationResult()
    object Expired : ValidationResult()
    object NotFound : ValidationResult()
    object NetworkError : ValidationResult()
}
