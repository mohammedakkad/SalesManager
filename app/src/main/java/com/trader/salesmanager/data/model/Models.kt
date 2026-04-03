package com.trader.salesmanager.data.model

import java.util.Date

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class PaymentStatus { PAID, UNPAID }

enum class PaymentMethodType { CASH, BANK, WALLET }

// ─── Domain Models ───────────────────────────────────────────────────────────

data class Customer(
    val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    // Computed fields populated by repository joins
    val totalDebt: Double = 0.0,
    val totalTransactions: Int = 0
)

data class PaymentMethod(
    val id: Int = 0,
    val name: String,
    val type: PaymentMethodType,
    val details: String = ""         // e.g. bank name, wallet name
)

data class Transaction(
    val id: Int = 0,
    val customerId: Int,
    val customerName: String = "",   // joined field
    val amount: Double,
    val status: PaymentStatus,
    val paymentMethodId: Int? = null,
    val paymentMethodName: String = "", // joined field
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val notes: String = ""
)

// ─── Summary DTO (used by Home & Reports) ────────────────────────────────────

data class DailySummary(
    val date: Long,
    val totalSales: Double,
    val totalPaid: Double,
    val totalUnpaid: Double,
    val transactionCount: Int
)
