package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.PaymentMethodEntity
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.PaymentMethod
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao
) : ActivationRepository {

    private val IS_ACTIVATED  = booleanPreferencesKey("is_activated")
    private val MERCHANT_CODE = stringPreferencesKey("merchant_code")
    private val db = FirebaseDatabase.getInstance()

    override suspend fun validateCode(code: String): Boolean = firebaseService.validateCode(code)

    override suspend fun isActivated(): Boolean =
        context.dataStore.data.map { it[IS_ACTIVATED] ?: false }.first()

    override suspend fun getMerchantCode(): String =
        context.dataStore.data.map { it[MERCHANT_CODE] ?: "" }.first()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.dataStore.edit {
            it[IS_ACTIVATED]  = activated
            it[MERCHANT_CODE] = code
        }
        if (activated && code.isNotEmpty()) {
            syncAllDataFromFirebase(code)
        }
    }

    // Fetch everything from Firebase and store locally on first activation
    private suspend fun syncAllDataFromFirebase(merchantCode: String) {
        try {
            val root = db.reference.child("merchants").child(merchantCode)

            // Customers
            val custSnap = root.child("customers").get().await()
            custSnap.children.forEach { snap ->
                val map = snap.value as? Map<*, *> ?: return@forEach
                customerDao.insertCustomer(CustomerEntity(
                    id        = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: return@forEach,
                    name      = map["name"] as? String ?: "",
                    phone     = map["phone"] as? String ?: "",
                    createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
                ))
            }

            // Payment methods (only if none exist locally)
            val pmCount = paymentMethodDao.getAllPaymentMethods().first().size
            if (pmCount == 0) {
                val pmSnap = root.child("payment_methods").get().await()
                pmSnap.children.forEach { snap ->
                    val map = snap.value as? Map<*, *> ?: return@forEach
                    paymentMethodDao.insertPaymentMethod(PaymentMethodEntity(
                        id   = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: return@forEach,
                        name = map["name"] as? String ?: "",
                        type = map["type"] as? String ?: PaymentType.OTHER.name
                    ))
                }
            }

            // Transactions
            val txSnap = root.child("transactions").get().await()
            txSnap.children.forEach { snap ->
                val map = snap.value as? Map<*, *> ?: return@forEach
                transactionDao.insertTransaction(TransactionEntity(
                    id              = (map["id"] as? Long) ?: snap.key?.toLongOrNull() ?: return@forEach,
                    customerId      = (map["customerId"] as? Long) ?: return@forEach,
                    amount          = (map["amount"] as? Double) ?: (map["amount"] as? Long)?.toDouble() ?: 0.0,
                    isPaid          = (map["isPaid"] as? Boolean) ?: false,
                    paymentMethodId = map["paymentMethodId"] as? Long,
                    note            = map["note"] as? String ?: "",
                    date            = (map["date"] as? Long) ?: System.currentTimeMillis(),
                    paidAt          = map["paidAt"] as? Long
                ))
            }
        } catch (e: Exception) {
            // offline-first: if sync fails, work with local data
        }
    }

    // Reset activation (called when merchant is disabled/deleted)
    suspend fun clearActivation() {
        context.dataStore.edit {
            it[IS_ACTIVATED]  = false
            it[MERCHANT_CODE] = ""
        }
    }
}