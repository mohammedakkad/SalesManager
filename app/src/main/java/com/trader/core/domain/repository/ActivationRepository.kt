package com.trader.core.domain.repository

import com.trader.core.domain.model.MerchantStatus
import kotlinx.coroutines.flow.Flow

interface ActivationRepository {
    suspend fun validateCode(code: String): Boolean
    suspend fun isActivated(): Boolean
    suspend fun getMerchantCode(): String
    suspend fun saveActivationStatus(activated: Boolean, code: String = "")
    suspend fun deactivate()
    fun observeMerchantStatus(): Flow<MerchantStatus?>
    // Check status on startup — returns null if offline (don't block)
    suspend fun verifyStatusOnStartup(): StartupStatus
}

enum class StartupStatus {
    ACTIVE,     // all good — enter app
    DISABLED,   // blocked by admin
    EXPIRED,    // subscription ended
    DELETED,    // code no longer exists
    NOT_ACTIVATED, // never activated
    OFFLINE     // no internet — allow entry with warning
}
