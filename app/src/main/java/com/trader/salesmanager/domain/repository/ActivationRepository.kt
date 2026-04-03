package com.trader.salesmanager.domain.repository

interface ActivationRepository {
    suspend fun validateCode(code: String): Boolean
    suspend fun isActivated(): Boolean
    suspend fun saveActivationStatus(activated: Boolean)
}