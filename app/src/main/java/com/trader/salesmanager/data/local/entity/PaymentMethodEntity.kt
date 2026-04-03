package com.trader.salesmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.salesmanager.domain.model.PaymentMethod
import com.trader.salesmanager.domain.model.PaymentType

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = PaymentType.OTHER.name
) {
    fun toDomain() = PaymentMethod(id = id, name = name, type = PaymentType.valueOf(type))
    companion object {
        fun fromDomain(m: PaymentMethod) = PaymentMethodEntity(id = m.id, name = m.name, type = m.type.name)
    }
}