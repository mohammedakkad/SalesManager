package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult
import kotlinx.coroutines.flow.Flow

interface ActivationRepository {
    suspend fun getMerchantCode(): String
    suspend fun verifyStatusOnStartup(): StartupStatus
    fun observeMerchantStatus(): Flow<MerchantStatus?>

    suspend fun validateCode(code: String): Boolean
    suspend fun validateCodeDetailed(code: String): ValidationResult
    suspend fun saveActivationStatus(activated: Boolean, code: String = "")
    suspend fun registerFree()
    suspend fun deactivate()

    suspend fun isActivated(): Boolean
    suspend fun isSelfRegistered(): Boolean

    fun observeMerchantCode(): Flow<String>
}
