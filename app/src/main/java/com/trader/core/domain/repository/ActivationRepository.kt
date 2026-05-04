package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult
import kotlinx.coroutines.flow.Flow

interface ActivationRepository {
    suspend fun validateCode(code: String): Boolean
    suspend fun validateCodeDetailed(code: String): ValidationResult
    suspend fun isActivated(): Boolean
    suspend fun getMerchantCode(): String
    /** Emits the merchant code whenever it changes (empty string = not activated) */
    fun observeMerchantCode(): Flow<String>
    suspend fun saveActivationStatus(activated: Boolean, code: String = "")
    suspend fun deactivate()
    fun observeMerchantStatus(): Flow<MerchantStatus?>
    suspend fun verifyStatusOnStartup(): StartupStatus

    // ── Freemium additions ───────────────────────────────────────
    /** One-time free registration — writes to Firestore, saves locally */
    suspend fun registerFree(deviceId: String)
    /** Reads tier from DataStore (fast, no network) */
    suspend fun getMerchantTier(): MerchantTier
    /** True if account was created via registerFree() */
    suspend fun isSelfRegistered(): Boolean
    /** Persist tier locally after remote fetch */
    suspend fun saveMerchantTier(tier: MerchantTier)
}
