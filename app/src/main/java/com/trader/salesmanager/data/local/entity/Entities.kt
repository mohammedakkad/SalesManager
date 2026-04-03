package com.trader.salesmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.salesmanager.data.model.Customer
import com.trader.salesmanager.data.model.PaymentMethod
import com.trader.salesmanager.data.model.PaymentMethodType
import com.trader.salesmanager.data.model.PaymentStatus
import com.trader.salesmanager.data.model.Transaction

// ─── Customer Entity ──────────────────────────────────────────────────────────

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(totalDebt: Double = 0.0, totalTransactions: Int = 0) = Customer(
        id = id,
        name = name,
        createdAt = createdAt,
        totalDebt = totalDebt,
        totalTransactions = totalTransactions
    )

    companion object {
        fun fromDomain(customer: Customer) = CustomerEntity(
            id = customer.id,
            name = customer.name,
            createdAt = customer.createdAt
        )
    }
}

// ─── PaymentMethod Entity ─────────────────────────────────────────────────────

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,               // PaymentMethodType.name
    val details: String = ""
) {
    fun toDomain() = PaymentMethod(
        id = id,
        name = name,
        type = PaymentMethodType.valueOf(type),
        details = details
    )

    companion object {
        fun fromDomain(pm: PaymentMethod) = PaymentMethodEntity(
            id = pm.id,
            name = pm.name,
            type = pm.type.name,
            details = pm.details
        )
    }
}

// ─── Transaction Entity ───────────────────────────────────────────────────────

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val amount: Double,
    val status: String,             // PaymentStatus.name
    val paymentMethodId: Int? = null,
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val notes: String = ""
) {
    fun toDomain(
        customerName: String = "",
        paymentMethodName: String = ""
    ) = Transaction(
        id = id,
        customerId = customerId,
        customerName = customerName,
        amount = amount,
        status = PaymentStatus.valueOf(status),
        paymentMethodId = paymentMethodId,
        paymentMethodName = paymentMethodName,
        date = date,
        paidAt = paidAt,
        notes = notes
    )

    companion object {
        fun fromDomain(t: Transaction) = TransactionEntity(
            id = t.id,
            customerId = t.customerId,
            amount = t.amount,
            status = t.status.name,
            paymentMethodId = t.paymentMethodId,
            date = t.date,
            paidAt = t.paidAt,
            notes = t.notes
        )
    }
}
