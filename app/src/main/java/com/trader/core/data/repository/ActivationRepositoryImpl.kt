package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

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

    override suspend fun validateCode(code: String) = firebaseService.validateCode(code)
    override suspend fun isActivated() = context.dataStore.data.map { it[IS_ACTIVATED] ?: false }.first()
    override suspend fun getMerchantCode() = context.dataStore.data.map { it[MERCHANT_CODE] ?: "" }.first()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.dataStore.edit {
            it[IS_ACTIVATED] = activated
            it[MERCHANT_CODE] = code
        }
        // On fresh activation: fetch ALL data from Firebase → store in Room
        if (activated && code.isNotEmpty()) {
            fetchAndStoreAllData(code)
        }
    }

    override suspend fun deactivate() {
        context.dataStore.edit { it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = "" }
        customerDao.deleteAll()
        transactionDao.deleteAll()
        paymentMethodDao.deleteAll()
    }

    /**
     * Listens to Firestore merchant document via callbackFlow.
     * Emits new status whenever admin changes it (ACTIVE → DISABLED / EXPIRED / deleted).
     */
    override fun observeMerchantStatus(): Flow<MerchantStatus?> = callbackFlow {
        val code = getMerchantCode()
        if (code.isEmpty()) { trySend(null); close(); return@callbackFlow }

        val listener = FirebaseFirestore.getInstance()
            .collection("merchants")
            .whereEqualTo("activationCode", code)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || snapshot.isEmpty) {
                    // Document deleted → treat as DISABLED
                    trySend(MerchantStatus.DISABLED)
                    return@addSnapshotListener
                }
                val doc = snapshot.documents.firstOrNull()
                val status = doc?.getString("status")?.let {
                    runCatching { MerchantStatus.valueOf(it) }.getOrNull()
                }
                trySend(status)
            }
        awaitClose { listener.remove() }
    }

    // ── private ──────────────────────────────────────────────────
    private suspend fun fetchAndStoreAllData(code: String) {
        val data = try { firebaseService.fetchAllData(code) } catch (e: Exception) { return }
        // Insert order matters: customers first (FK parent), then methods, then transactions
        data.customers.forEach { c ->
            try { customerDao.insertCustomer(CustomerEntity.fromDomain(c)) } catch (_: Exception) {}
        }
        data.paymentMethods.forEach { m ->
            try { paymentMethodDao.insertPaymentMethod(PaymentMethodEntity.fromDomain(m)) } catch (_: Exception) {}
        }
        data.transactions.forEach { t ->
            try { transactionDao.insertTransaction(TransactionEntity.fromDomain(t)) } catch (_: Exception) {}
        }
    }
}
