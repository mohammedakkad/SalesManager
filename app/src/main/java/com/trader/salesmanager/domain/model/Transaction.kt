package com.trader.salesmanager.domain.model

data class Transaction(
    val id: Long = 0,
    val customerId: Long,
    val customerName: String = "",
    val amount: Double,
    val isPaid: Boolean,
    val paymentMethodId: Long? = null,
    val paymentMethodName: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paidAt: Long? = null
)