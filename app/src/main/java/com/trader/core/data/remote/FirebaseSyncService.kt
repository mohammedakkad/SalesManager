package com.trader.core.data.remote

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

class FirebaseSyncService {
    private val db = FirebaseDatabase.getInstance()

    // ── Type-safe Int/Long helpers ───────────────────────────────
    private fun Any?.asLong(): Long? = when (this) {
        is Long -> this; is Int -> toLong(); is Double -> toLong(); else -> null
    }
    private fun Any?.asDouble(): Double? = when (this) {
        is Double -> this; is Long -> toDouble(); is Int -> toDouble(); else -> null
    }

    suspend fun validateCodeDetailed(code: String): ValidationResult {
        return try {
            val snap = db.reference.child("activation_codes").child(code).get().await()
            if (!snap.exists()) return ValidationResult.NotFound
            val boolVal = snap.getValue(Boolean::class.java)
            if (boolVal != null) return if (boolVal) ValidationResult.Active else ValidationResult.Disabled
            val map = snap.value as? Map<*, *> ?: return ValidationResult.Active
            when (map["status"] as? String) {
                "ACTIVE" -> ValidationResult.Active
                "DISABLED" -> ValidationResult.Disabled
                "EXPIRED" -> ValidationResult.Expired
                "DELETED" -> ValidationResult.NotFound
                else -> ValidationResult.Active
            }
        } catch (e: Exception) {
            ValidationResult.NetworkError
        }
    }


    // ── Activation ───────────────────────────────────────────────
    suspend fun validateCode(code: String): Boolean =
    validateCodeDetailed(code) == ValidationResult.Active

    // Check current status without full validation (used on app startup)
    suspend fun getCodeStatus(code: String): String? {
        return try {
            val snap = db.reference.child("activation_codes").child(code).get().await()
            if (!snap.exists()) return "DELETED"
            val boolVal = snap.getValue(Boolean::class.java)
            if (boolVal != null) return if (boolVal) "ACTIVE" else "DISABLED"
            val map = snap.value as? Map<*, *> ?: return "ACTIVE"
            map["status"] as? String ?: "ACTIVE"
        } catch (e: Exception) {
            null
        } // null = offline, don't block
    }

    // ── One-time full fetch on activation ────────────────────────
    suspend fun fetchAllData(merchantCode: String): MerchantData {
        val root = db.reference.child("merchants").child(merchantCode)
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
        val transactions = try {
            root.child("transactions").get().await().children.mapNotNull {
                snap ->
                val m = snap.value as? Map<*, *> ?: return@mapNotNull null
                AppTransaction(
                    id = m["id"].asLong() ?: snap.key?.toLongOrNull() ?: return@mapNotNull null,
                    customerId = m["customerId"].asLong() ?: return@mapNotNull null,
                    amount = m["amount"].asDouble() ?: return@mapNotNull null,
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
        return MerchantData(customers, transactions, paymentMethods)
    }

    // ── Real-time streams (callbackFlow) ─────────────────────────
    fun observeCustomers(merchantCode: String): Flow<List<Customer>> = callbackFlow {
        val ref = db.reference.child("merchants").child(merchantCode).child("customers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children.mapNotNull {
                    child ->
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
                trySend(snap.children.mapNotNull {
                    child ->
                    val m = child.value as? Map<*, *> ?: return@mapNotNull null
                    runCatching {
                        AppTransaction(
                            id = m["id"].asLong() ?: child.key?.toLongOrNull() ?: return@mapNotNull null,
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

    // ── Push helpers ─────────────────────────────────────────────
    fun pushCustomer(merchantCode: String, c: Customer) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(c.id.toString())
        .setValue(mapOf("id" to c.id, "name" to c.name, "phone" to c.phone, "createdAt" to c.createdAt))
    }
    fun deleteCustomer(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(id.toString()).removeValue()
    }
    fun pushTransaction(merchantCode: String, t: AppTransaction) {
        db.reference.child("merchants").child(merchantCode).child("transactions").child(t.id.toString())
        .setValue(mapOf("id" to t.id, "customerId" to t.customerId, "amount" to t.amount,
            "isPaid" to t.isPaid, "paymentMethodId" to t.paymentMethodId,
            "note" to t.note, "date" to t.date, "paidAt" to t.paidAt))
    }
    fun deleteTransaction(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("transactions").child(id.toString()).removeValue()
    }
    fun pushPaymentMethod(merchantCode: String, m: PaymentMethod) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods").child(m.id.toString())
        .setValue(mapOf("id" to m.id, "name" to m.name, "type" to m.type.name))
    }
    fun deletePaymentMethod(merchantCode: String, id: Long) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods").child(id.toString()).removeValue()
    }
}

data class MerchantData(
    val customers: List<Customer>,
    val transactions: List<AppTransaction>,
    val paymentMethods: List<PaymentMethod>
)

sealed class ValidationResult {
    object Active : ValidationResult()
    object Disabled : ValidationResult()
    object Expired : ValidationResult()
    object NotFound : ValidationResult()
    object NetworkError : ValidationResult()
    }