package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
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
}
