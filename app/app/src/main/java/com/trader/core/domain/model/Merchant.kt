package com.trader.core.domain.model

import com.google.firebase.Timestamp

data class Merchant(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val activationCode: String = "",
    val status: MerchantStatus = MerchantStatus.ACTIVE,
    val isPermanent: Boolean = true,
    val expiryDate: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val lastSeen: Timestamp? = null
)

enum class MerchantStatus { ACTIVE, EXPIRED, DISABLED }
