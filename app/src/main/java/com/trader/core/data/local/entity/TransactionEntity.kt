package com.trader.core.data.local.entity

import androidx.room.*
import com.trader.core.domain.model.PaymentType
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
    val customerId: Long,
    val amount: Double,
    val isPaid: Boolean,
    val paymentMethodId: Long? = null,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val paymentType: String = PaymentType.DEBT.name,  // ← v4
    val hasItems: Boolean = false                      // ← v4
) {
    fun toDomain(customerName: String = "", paymentMethodName: String = "") = Transaction(
        id = id, customerId = customerId, customerName = customerName,
        amount = amount, isPaid = isPaid,
        paymentMethodId = paymentMethodId, paymentMethodName = paymentMethodName,
        paymentType = runCatching { PaymentType.valueOf(paymentType) }.getOrDefault(PaymentType.DEBT),
        note = note, date = date, paidAt = paidAt, hasItems = hasItems
    )
    companion object {
        fun fromDomain(t: Transaction) = TransactionEntity(
            id = t.id, customerId = t.customerId, amount = t.amount,
            isPaid = t.isPaid, paymentMethodId = t.paymentMethodId,
            note = t.note, date = t.date, paidAt = t.paidAt,
            paymentType = t.paymentType.name, hasItems = t.hasItems
        )
    }
}
