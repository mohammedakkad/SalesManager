package com.trader.salesmanager.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.trader.salesmanager.domain.model.Customer
import com.trader.salesmanager.domain.model.PaymentMethod
import com.trader.salesmanager.domain.model.PaymentType
import com.trader.salesmanager.domain.model.Transaction
import kotlinx.coroutines.tasks.await

class FirebaseSyncService {

    private val db = FirebaseDatabase.getInstance()

    // ── Activation ──────────────────────────────────────────────
    suspend fun validateCode(code: String): Boolean {
        return try {
            val snap = db.reference.child("activation_codes").child(code).get().await()
            snap.exists() && snap.getValue(Boolean::class.java) == true
        } catch (e: Exception) { false }
    }

    // ── Fetch all merchant data on first login ───────────────────
    suspend fun fetchAllData(merchantCode: String): MerchantData {
        val root = db.reference.child("merchants").child(merchantCode)

        val customers = try {
            root.child("customers").get().await().children.mapNotNull { snap ->
                val map = snap.value as? Map<*, *> ?: return@mapNotNull null
                Customer(
                    id        = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: 0L,
                    name      = map["name"] as? String ?: "",
                    createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) { emptyList() }

        val transactions = try {
            root.child("transactions").get().await().children.mapNotNull { snap ->
                val map = snap.value as? Map<*, *> ?: return@mapNotNull null
                Transaction(
                    id              = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: 0L,
                    customerId      = (map["customerId"] as? Long) ?: 0L,
                    amount          = (map["amount"] as? Double) ?: (map["amount"] as? Long)?.toDouble() ?: 0.0,
                    isPaid          = (map["isPaid"] as? Boolean) ?: false,
                    paymentMethodId = map["paymentMethodId"] as? Long,
                    note            = map["note"] as? String ?: "",
                    date            = (map["date"] as? Long) ?: System.currentTimeMillis(),
                    paidAt          = map["paidAt"] as? Long
                )
            }
        } catch (e: Exception) { emptyList() }

        val paymentMethods = try {
            root.child("payment_methods").get().await().children.mapNotNull { snap ->
                val map = snap.value as? Map<*, *> ?: return@mapNotNull null
                PaymentMethod(
                    id   = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: 0L,
                    name = map["name"] as? String ?: "",
                    type = PaymentType.valueOf(map["type"] as? String ?: PaymentType.OTHER.name)
                )
            }
        } catch (e: Exception) { emptyList() }

        return MerchantData(customers, transactions, paymentMethods)
    }

    // ── Customers ────────────────────────────────────────────────
    fun pushCustomer(merchantCode: String, customer: Customer) {
        db.reference.child("merchants").child(merchantCode)
            .child("customers").child(customer.id.toString())
            .setValue(mapOf("id" to customer.id, "name" to customer.name, "createdAt" to customer.createdAt))
    }

    fun deleteCustomer(merchantCode: String, customerId: Long) {
        db.reference.child("merchants").child(merchantCode)
            .child("customers").child(customerId.toString()).removeValue()
    }

    // ── Transactions ─────────────────────────────────────────────
    fun pushTransaction(merchantCode: String, transaction: Transaction) {
        db.reference.child("merchants").child(merchantCode)
            .child("transactions").child(transaction.id.toString())
            .setValue(mapOf(
                "id"              to transaction.id,
                "customerId"      to transaction.customerId,
                "amount"          to transaction.amount,
                "isPaid"          to transaction.isPaid,
                "paymentMethodId" to transaction.paymentMethodId,
                "note"            to transaction.note,
                "date"            to transaction.date,
                "paidAt"          to transaction.paidAt
            ))
    }

    fun deleteTransaction(merchantCode: String, transactionId: Long) {
        db.reference.child("merchants").child(merchantCode)
            .child("transactions").child(transactionId.toString()).removeValue()
    }

    // ── Payment Methods ──────────────────────────────────────────
    fun pushPaymentMethod(merchantCode: String, method: PaymentMethod) {
        db.reference.child("merchants").child(merchantCode)
            .child("payment_methods").child(method.id.toString())
            .setValue(mapOf("id" to method.id, "name" to method.name, "type" to method.type.name))
    }

    fun deletePaymentMethod(merchantCode: String, methodId: Long) {
        db.reference.child("merchants").child(merchantCode)
            .child("payment_methods").child(methodId.toString()).removeValue()
    }
}

data class MerchantData(
    val customers: List<Customer>,
    val transactions: List<Transaction>,
    val paymentMethods: List<PaymentMethod>
)
