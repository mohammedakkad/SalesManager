package com.trader.salesmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trader.salesmanager.data.local.dao.CustomerDao
import com.trader.salesmanager.data.local.dao.PaymentMethodDao
import com.trader.salesmanager.data.local.dao.TransactionDao
import com.trader.salesmanager.data.local.entity.CustomerEntity
import com.trader.salesmanager.data.local.entity.PaymentMethodEntity
import com.trader.salesmanager.data.local.entity.TransactionEntity
import com.trader.salesmanager.data.remote.FirebaseSyncService
import com.trader.salesmanager.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao
) : ActivationRepository {

    private val IS_ACTIVATED      = booleanPreferencesKey("is_activated")
    private val MERCHANT_CODE_KEY = stringPreferencesKey("merchant_code")

    override suspend fun validateCode(code: String): Boolean {
        return firebaseService.validateCode(code)
    }

    override suspend fun isActivated(): Boolean =
        context.dataStore.data.map { it[IS_ACTIVATED] ?: false }.first()

    override suspend fun getMerchantCode(): String =
        context.dataStore.data.map { it[MERCHANT_CODE_KEY] ?: "" }.first()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.dataStore.edit {
            it[IS_ACTIVATED]      = activated
            it[MERCHANT_CODE_KEY] = code
        }
        if (activated && code.isNotEmpty()) {
            // Fetch all merchant data from Firebase and store locally
            fetchAndStoreAllData(code)
        }
    }

    private suspend fun fetchAndStoreAllData(merchantCode: String) {
        try {
            val data = firebaseService.fetchAllData(merchantCode)

            // Store customers
            data.customers.forEach { customer ->
                customerDao.insertCustomer(CustomerEntity.fromDomain(customer))
            }

            // Store payment methods
            data.paymentMethods.forEach { method ->
                paymentMethodDao.insertPaymentMethod(PaymentMethodEntity.fromDomain(method))
            }

            // Store transactions
            data.transactions.forEach { transaction ->
                transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
            }
        } catch (e: Exception) {
            // If Firebase fetch fails, app still works with local data (offline-first)
        }
    }
}
