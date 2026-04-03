package com.trader.salesmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.salesmanager.domain.model.Transaction

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
    val customerId: Long,
    val amount: Double,
    val isPaid: Boolean,
    val paymentMethodId: Long? = null,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null
) {
    fun toDomain(customerName: String = "", paymentMethodName: String = "") = Transaction(
        id = id, customerId = customerId, customerName = customerName,
        amount = amount, isPaid = isPaid, paymentMethodId = paymentMethodId,
        paymentMethodName = paymentMethodName, note = note, date = date, paidAt = paidAt
    )
    companion object {
        fun fromDomain(t: Transaction) = TransactionEntity(
            id = t.id, customerId = t.customerId, amount = t.amount,
            isPaid = t.isPaid, paymentMethodId = t.paymentMethodId,
            note = t.note, date = t.date, paidAt = t.paidAt
        )
    }
}