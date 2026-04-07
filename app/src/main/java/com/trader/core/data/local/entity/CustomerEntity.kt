package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Customer

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Customer(id = id, name = name, phone = phone, createdAt = createdAt)
    companion object {
        fun fromDomain(c: Customer) = CustomerEntity(id = c.id, name = c.name, phone = c.phone, createdAt = c.createdAt)
    }
}
