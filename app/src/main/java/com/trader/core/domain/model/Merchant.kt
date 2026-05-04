package com.trader.core.domain.model

import com.google.firebase.Timestamp

enum class MerchantStatus { ACTIVE, EXPIRED, DISABLED }
enum class MerchantTier   { FREE, PREMIUM }            // ← NEW

data class Merchant(
    val id: String               = "",
    val name: String             = "",
    val phone: String            = "",
    val activationCode: String   = "",
    val status: MerchantStatus   = MerchantStatus.ACTIVE,
    val tier: MerchantTier       = MerchantTier.FREE,  // ← NEW
    val isPermanent: Boolean     = true,
    val isSelfRegistered: Boolean = false,              // ← NEW: free sign-up
    val expiryDate: Timestamp?   = null,
    val createdAt: Timestamp?    = null,
    val lastSeen: Timestamp?     = null
)
