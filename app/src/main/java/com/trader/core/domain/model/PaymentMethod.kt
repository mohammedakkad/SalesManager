package com.trader.core.domain.model

data class PaymentMethod(
    val id: Long = 0,
    val name: String,
    val type: PaymentType = PaymentType.OTHER
)

enum class PaymentType { BANK, WALLET, CASH, OTHER }
