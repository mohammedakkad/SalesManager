package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult
import kotlinx.coroutines.flow.Flow

interface ActivationRepository {
    suspend fun getMerchantCode(): String
    suspend fun verifyStatusOnStartup(): StartupStatus
    fun observeMerchantStatus(): Flow<MerchantStatus?>

    /** Reads tier from DataStore (fast, no network) */
    suspend fun getMerchantTier(): MerchantTier
    fun observeMerchantTier(): Flow<MerchantTier>
    /** Persist tier locally after remote fetch */
    suspend fun saveMerchantTier(tier: MerchantTier)

    suspend fun validateCode(code: String): Boolean
    suspend fun validateCodeDetailed(code: String): ValidationResult
    suspend fun saveActivationStatus(activated: Boolean, code: String = "")
    suspend fun registerFree()
    suspend fun deactivate()

    suspend fun isActivated(): Boolean
    /** True if account was created via registerFree() */
    suspend fun isSelfRegistered(): Boolean

    /** Emits the merchant code whenever it changes (empty string = not activated) */
    fun observeMerchantCode(): Flow<String>
}
