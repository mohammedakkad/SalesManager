package com.trader.core.domain.model

import java.util.UUID

enum class EmployeeRole {
    ADMIN,
    CASHIER
}

data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val merchantId: String = "",
    val name: String,
    val pinCode: String,
    val role: EmployeeRole = EmployeeRole.CASHIER,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
