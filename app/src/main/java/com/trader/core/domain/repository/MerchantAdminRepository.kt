package com.trader.core.domain.repository

import com.google.firebase.Timestamp
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.model.MerchantStatus
import kotlinx.coroutines.flow.Flow

interface MerchantAdminRepository {
    fun getAllMerchants(): Flow<List<Merchant>>
    suspend fun getMerchantById(id: String): Merchant?
    suspend fun addMerchant(merchant: Merchant): String
    suspend fun updateMerchant(merchant: Merchant)
    suspend fun deleteMerchant(id: String)
    suspend fun setMerchantStatus(id: String, status: MerchantStatus)
    /** Adjusts expiry date by deltaDays (+extend / -reduce). No-op for permanent merchants. */
    suspend fun adjustExpiry(id: String, deltaDays: Int)
    /** Converts subscription type: permanent ↔ temporary. */
    suspend fun setSubscriptionType(id: String, isPermanent: Boolean, expiryDate: Timestamp?)
}
