package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.SyncStatus

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // ✅ عمود جديد — default "SYNCED" حتى لا تُعامَل السجلات القديمة كـ PENDING
    val syncStatus: String = "SYNCED"
) {
    fun toDomain() = Customer(
        id = id, name = name, phone = phone, createdAt = createdAt,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.SYNCED)
    )
    companion object {
        fun fromDomain(c: Customer) = CustomerEntity(
            id = c.id, name = c.name, phone = c.phone, createdAt = c.createdAt,
            syncStatus = c.syncStatus.name
        )
    }
}
