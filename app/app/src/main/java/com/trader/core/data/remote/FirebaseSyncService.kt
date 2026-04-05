package com.trader.core.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.trader.core.domain.model.*
import kotlinx.coroutines.tasks.await

class FirebaseSyncService {
    private val db = FirebaseDatabase.getInstance()

    suspend fun validateCode(code: String): Boolean = try {
        val snap = db.reference.child("activation_codes").child(code).get().await()
        snap.exists() && snap.getValue(Boolean::class.java) == true
    } catch (e: Exception) { false }

    fun pushCustomer(merchantCode: String, customer: Customer) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(customer.id.toString())
            .setValue(mapOf("id" to customer.id, "name" to customer.name, "createdAt" to customer.createdAt))
    }
    fun deleteCustomer(merchantCode: String, customerId: Long) {
        db.reference.child("merchants").child(merchantCode).child("customers").child(customerId.toString()).removeValue()
    }
    fun pushTransaction(merchantCode: String, transaction: Transaction) {
        db.reference.child("merchants").child(merchantCode).child("transactions").child(transaction.id.toString())
            .setValue(mapOf("id" to transaction.id, "customerId" to transaction.customerId, "amount" to transaction.amount,
                "isPaid" to transaction.isPaid, "paymentMethodId" to transaction.paymentMethodId,
                "note" to transaction.note, "date" to transaction.date, "paidAt" to transaction.paidAt))
    }
    fun deleteTransaction(merchantCode: String, transactionId: Long) {
        db.reference.child("merchants").child(merchantCode).child("transactions").child(transactionId.toString()).removeValue()
    }
    fun pushPaymentMethod(merchantCode: String, method: PaymentMethod) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods").child(method.id.toString())
            .setValue(mapOf("id" to method.id, "name" to method.name, "type" to method.type.name))
    }
    fun deletePaymentMethod(merchantCode: String, methodId: Long) {
        db.reference.child("merchants").child(merchantCode).child("payment_methods").child(methodId.toString()).removeValue()
    }
}
