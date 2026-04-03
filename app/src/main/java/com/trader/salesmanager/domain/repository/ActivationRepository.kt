package com.trader.salesmanager.domain.repository

interface ActivationRepository {
    suspend fun validateCode(code: String): Boolean
    suspend fun isActivated(): Boolean
    suspend fun getMerchantCode(): String
    suspend fun saveActivationStatus(activated: Boolean, code: String = "")
}
