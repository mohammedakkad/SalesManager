package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao
) : ActivationRepository {
    private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
    private val MERCHANT_CODE = stringPreferencesKey("merchant_code")

    override suspend fun validateCode(code: String): Boolean = firebaseService.validateCode(code)
    override suspend fun isActivated(): Boolean = context.dataStore.data.map { it[IS_ACTIVATED] ?: false }.first()
    override suspend fun getMerchantCode(): String = context.dataStore.data.map { it[MERCHANT_CODE] ?: "" }.first()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.dataStore.edit { it[IS_ACTIVATED] = activated; it[MERCHANT_CODE] = code }
    }
}
