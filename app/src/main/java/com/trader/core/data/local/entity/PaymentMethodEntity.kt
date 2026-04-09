package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.model.PaymentType

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = PaymentType.OTHER.name
) {
    fun toDomain() = PaymentMethod(id = id, name = name, type = PaymentType.valueOf(type))

    companion object {
        fun fromDomain(paymentMethod: PaymentMethod) =
            PaymentMethodEntity(id = paymentMethod.id, name = paymentMethod.name, type = paymentMethod.type.name)
    }
}
