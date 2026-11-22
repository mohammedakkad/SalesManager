package com.trader.core.domain.model

data class SubscriptionPlan(
    val id: String,
    val arabicLabel: String,
    val priceLabel: String,
    val periodLabel: String,
    val savingBadge: String? = null,
    val isRecommended: Boolean = false
)

data class SubscriptionPaymentMethod(
    val id: String,
    val name: String,
    val accountNumber: String
)