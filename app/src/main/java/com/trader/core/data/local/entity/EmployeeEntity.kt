package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.EmployeeRole
import com.trader.core.domain.model.SyncStatus

@Entity(
    tableName = "employees",
    indices = [Index("merchantId")]
)
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val name: String,
    val pinCode: String,
    val role: String,
    val createdAt: Long,
    val syncStatus: String = "PENDING"
) {
    fun toDomain() = Employee(
        id = id,
        merchantId = merchantId,
        name = name,
        pinCode = pinCode,
        role = runCatching { EmployeeRole.valueOf(role) }.getOrDefault(EmployeeRole.CASHIER),
        createdAt = createdAt,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.PENDING)
    )

    companion object {
        fun fromDomain(e: Employee) = EmployeeEntity(
            id = e.id,
            merchantId = e.merchantId,
            name = e.name,
            pinCode = e.pinCode,
            role = e.role.name,
            createdAt = e.createdAt,
            syncStatus = e.syncStatus.name
        )
    }
}
