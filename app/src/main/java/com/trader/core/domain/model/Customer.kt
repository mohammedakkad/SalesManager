package com.trader.core.domain.model

data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)