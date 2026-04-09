package com.trader.core.data.local.entity

import androidx.room.*
import com.trader.core.domain.model.Transaction

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = CustomerEntity::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("customerId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long, val amount: Double, val isPaid: Boolean,
    val paymentMethodId: Long? = null, val note: String = "",
    val date: Long = System.currentTimeMillis(), val paidAt: Long? = null
) {
    fun toDomain(customerName: String = "", paymentMethodName: String = "") = Transaction(
        id = id, customerId = customerId, customerName = customerName, amount = amount,
        isPaid = isPaid, paymentMethodId = paymentMethodId, paymentMethodName = paymentMethodName,
        note = note, date = date, paidAt = paidAt
    )

    companion object {
        fun fromDomain(transaction: Transaction) = TransactionEntity(
            id = transaction.id, customerId = transaction.customerId, amount = transaction.amount, isPaid = transaction.isPaid,
            paymentMethodId = transaction.paymentMethodId, note = transaction.note, date = transaction.date, paidAt = transaction.paidAt
        )
    }
}
