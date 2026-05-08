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
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.SubscriptionStatus
import com.trader.core.domain.model.ReturnInvoice
import com.trader.core.domain.model.ReturnItem
import com.trader.core.domain.model.ReturnType
import com.trader.core.domain.model.TransactionReturnStatus

class FirebaseSyncService {
    private val db = FirebaseDatabase.getInstance()

    private fun Any?.asLong(): Long? = when (this) {
        is Long -> this; is Int -> toLong(); is Double -> toLong(); else -> null
    }

    private fun Any?.asDouble(): Double? = when (this) {
        is Double -> this; is Long -> toDouble(); is Int -> toDouble(); else -> null
    }

    suspend fun validateCodeDetailed(code: String): ValidationResult {
        return try {
            val snap = withTimeoutOrNull(8_000) {
                db.reference.child("activation_codes").child(code).get().await()
            } ?: return ValidationResult.NetworkError

            if (!snap.exists()) return ValidationResult.NotFound
            val value = snap.value

            if (value is Boolean) {
                return if (value) ValidationResult.Active else ValidationResult.Disabled
            }

            if (value is String) {
                return when (value.uppercase()) {
                    "ACTIVE", "TRUE" -> ValidationResult.Active
                    "DISABLED", "FALSE" -> ValidationResult.Disabled
                    "EXPIRED" -> ValidationResult.Expired
                    "DELETED" -> ValidationResult.NotFound
                    else -> ValidationResult.Active
                }
            }

            val map = value as? Map<*, *> ?: return ValidationResult.Active
            val explicitStatus = map["status"] as? String ?: "ACTIVE"

            if (explicitStatus.uppercase() == "DISABLED") return ValidationResult.Disabled
            if (explicitStatus.uppercase() == "EXPIRED") return ValidationResult.Expired
            if (explicitStatus.uppercase() == "DELETED") return ValidationResult.NotFound

            val expiryMs = (map["subscriptionExpiry"] as? Number)?.toLong()
            if (expiryMs != null) {
                val now = System.currentTimeMillis()
                val gracePeriodMs = 3L * 24 * 60 * 60 * 1000
                if (now > expiryMs + gracePeriodMs) {
                    return ValidationResult.Expired
                }
            }

            return ValidationResult.Active
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

    suspend fun fetchMerchantActivationData(merchantCode: String): DataSnapshot {
        return db.getReference(PATH_ACTIVATION_CODES).child(merchantCode).get().await()
    }


    suspend fun validateCode(code: String): Boolean =
    validateCodeDetailed(code) == ValidationResult.Active

    suspend fun getCodeStatus(code: String): String? {
        return try {
            val snap = withTimeoutOrNull(8_000) {
                db.reference.child("activation_codes").child(code).get().await()
            } ?: return null

            if (!snap.exists()) return "DELETED"
            val value = snap.value

            if (value is Boolean) {
                return if (value) "ACTIVE" else "DISABLED"
            }

            if (value is String) {
                return if (value.equals("true", ignoreCase = true)) "ACTIVE" else value.uppercase()
            }

            val map = value as? Map<*, *> ?: return "ACTIVE"
            val explicitStatus = map["status"] as? String ?: "ACTIVE"

            if (explicitStatus.uppercase() == "DISABLED") return "DISABLED"
            if (explicitStatus.uppercase() == "EXPIRED") return "EXPIRED"
            if (explicitStatus.uppercase() == "DELETED") return "DELETED"

            val expiryMs = (map["subscriptionExpiry"] as? Number)?.toLong()
            if (expiryMs != null) {
                val now = System.currentTimeMillis()
                val gracePeriodMs = 3L * 24 * 60 * 60 * 1000
                if (now > expiryMs + gracePeriodMs) {
                    return "EXPIRED"
                }
            }

            return "ACTIVE"
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllData(merchantCode: String): MerchantData {
        val root = db.reference.child("merchants").child(merchantCode)

        // 1. Fetch Customers
        val customers = try {
            root.child("customers").get().await().children.mapNotNull {
                snap ->
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

        // 2. Fetch Payment Methods
        val paymentMethods = try {
            root.child("payment_methods").get().await().children.mapNotNull {
                snap ->
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

        // 3. Fetch Transactions
        val transactions = try {
            root.child("transactions").get().await().children.mapNotNull {
                snap ->
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
                    paidAt = m["paidAt"].asLong()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        // 4. Fetch Returns
        val returns = try {
            root.child("return_invoices").get().await().children.mapNotNull {
                snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                val invoiceId = m["id"] as? String ?: snap.key ?: return@mapNotNull null
                val invoice = ReturnInvoice(
                    id = invoiceId,
                    originalTransactionId = m["originalTransactionId"].asLong() ?: return@mapNotNull null,
                    merchantId = merchantCode,
                    returnType = runCatching {
                        ReturnType.valueOf(m["returnType"] as? String ?: "")
                    }.getOrDefault(ReturnType.PARTIAL),
                    totalRefund = m["totalRefund"].asDouble() ?: 0.0,
                    note = m["note"] as? String ?: "",
                    createdAt = m["createdAt"].asLong() ?: System.currentTimeMillis()
                )

                val itemsMap = m["items"] as? Map<*, *> ?: emptyMap<Any, Any>()
                // ✅ استخدام mapNotNull مع (key, itemRaw) للحصول على الـ Key كبديل في حال فقدان الـ id
                val items = itemsMap.mapNotNull {
                    (key, itemRaw) ->
                    val im = itemRaw as? Map<*, *> ?: return@mapNotNull null
                    ReturnItem(
                        id = im["id"] as? String ?: key.toString(), // ✅ الاعتماد على الـ Key كخيار بديل قوي
                        returnInvoiceId = invoiceId,
                        productId = im["productId"] as? String ?: "",
                        productName = im["productName"] as? String ?: "",
                        unitId = im["unitId"] as? String ?: return@mapNotNull null,
                        unitLabel = im["unitLabel"] as? String ?: "",
                        originalQuantity = im["originalQuantity"].asDouble() ?: 0.0,
                        returnedQuantity = im["returnedQty"].asDouble() ?: 0.0,
                        costPricePerUnit = im["costPricePerUnit"].asDouble() ?: 0.0,
                        lostProfit = im["lostProfit"].asDouble() ?: 0.0,
                        pricePerUnit = im["pricePerUnit"].asDouble() ?: 0.0,
                        totalRefund = im["totalRefund"].asDouble() ?: 0.0
                    )
                }
                Pair(invoice, items)
            }
        } catch (e: Exception) {
            emptyList()
        }

        // 5. Fetch Invoice Items
        val invoiceItems = try {
            root.child("invoice_items").get().await().children.mapNotNull {
                snap ->
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

        return MerchantData(customers, transactions, paymentMethods, returns, invoiceItems)
    }

    fun observeCustomers(merchantCode: String): Flow<List<Customer>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("customers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull {
                    child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        Customer(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull()
                            ?: return@mapNotNull null,
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
                trySend(snap.children.mapNotNull {
                    child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        AppTransaction(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull()
                            ?: return@mapNotNull null,
                            customerId = m["customerId"].asLong() ?: return@mapNotNull null,
                            amount = m["amount"].asDouble() ?: return@mapNotNull null,
                            isPaid = m["isPaid"] as? Boolean ?: false,
                            paymentMethodId = m["paymentMethodId"].asLong(),
                            note = m["note"] as? String ?: "",
                            date = m["date"].asLong() ?: System.currentTimeMillis(),
                            paidAt = m["paidAt"].asLong()
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
                trySend(snap.children.mapNotNull {
                    child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        PaymentMethod(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull()
                            ?: return@mapNotNull null,
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

    fun pushCustomer(merchantCode: String, c: Customer) {
        db.reference.child("merchants").child(merchantCode).child("customers")
        .child(c.id.toString())
        .setValue(
            mapOf(
                "id" to c.id,
                "name" to c.name,
                "phone" to c.phone,
                "createdAt" to c.createdAt
            )
        )
    }

    fun deleteCustomer(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(id.toString())
        .removeValue()
    }

    suspend fun pushTransaction(merchantCode: String, t: AppTransaction) {
        db.reference.child("merchants").child(merchantCode).child("transactions")
        .child(t.id.toString())
        .setValue(
            mapOf(
                "id" to t.id, "customerId" to t.customerId, "amount" to t.amount,
                "originalAmount" to t.originalAmount, "returnStatus" to t.returnStatus.name,
                "isPaid" to t.isPaid, "paymentMethodId" to t.paymentMethodId,
                "note" to t.note, "date" to t.date, "paidAt" to t.paidAt
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
                        "totalRefund" to it.totalRefund
                    )
                }
            )).await()
        }
    }

    suspend fun deleteTransaction(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("transactions")
        .child(id.toString()).removeValue().await()
    }

    fun pushPaymentMethod(merchantCode: String, m: PaymentMethod) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods")
        .child(m.id.toString())
        .setValue(mapOf("id" to m.id, "name" to m.name, "type" to m.type.name))
    }

    fun deletePaymentMethod(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods")
        .child(id.toString()).removeValue()
    }

    fun observeInvoiceItems(merchantCode: String): Flow<List<InvoiceItem>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("invoice_items")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull {
                    child ->
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

    suspend fun pushInvoiceItems(merchantCode: String, items: List<InvoiceItem>) {
        val ref = db.reference.child("merchants").child(merchantCode).child("invoice_items")
        items.forEach {
            item ->
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
    val invoiceItems: List<InvoiceItem> = emptyList()
)

sealed class ValidationResult {
    object Active : ValidationResult()
    object Disabled : ValidationResult()
    object Expired : ValidationResult()
    object NotFound : ValidationResult()
    object NetworkError : ValidationResult()
    }