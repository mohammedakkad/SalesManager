package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import com.trader.core.data.local.appDataStore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult


class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val productDao: ProductDao,
    private val productFirestoreService: ProductFirestoreService
) : ActivationRepository {

    private val IS_ACTIVATED       = booleanPreferencesKey("is_activated")
    private val MERCHANT_CODE      = stringPreferencesKey("merchant_code")
    private val MERCHANT_TIER      = stringPreferencesKey("merchant_tier")
    private val IS_SELF_REGISTERED = booleanPreferencesKey("is_self_registered")

    override suspend fun validateCode(code: String) = firebaseService.validateCode(code)

    override suspend fun validateCodeDetailed(code: String): ValidationResult =
        firebaseService.validateCodeDetailed(code)

    override suspend fun isActivated() =
        context.appDataStore.data.map { it[IS_ACTIVATED] ?: false }.first()

    override suspend fun getMerchantCode() =
        context.appDataStore.data.map { it[MERCHANT_CODE] ?: "" }.first()

    override fun observeMerchantCode(): Flow<String> =
        context.appDataStore.data.map { it[MERCHANT_CODE] ?: "" }.distinctUntilChanged()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.appDataStore.edit {
            it[IS_ACTIVATED]  = activated
            it[MERCHANT_CODE] = code
        }
        if (activated && code.isNotEmpty()) {
            fetchAndStoreAllData(code)
        }
    }

    override suspend fun deactivate() {
        context.appDataStore.edit {
            it[IS_ACTIVATED]  = false
            it[MERCHANT_CODE] = ""
        }
        customerDao.deleteAll()
        transactionDao.deleteAll()
        paymentMethodDao.deleteAll()
        productDao.deleteAllProducts()
    }

    override fun observeMerchantStatus(): Flow<MerchantStatus?> = callbackFlow {
        val code = getMerchantCode()
        if (code.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseFirestore.getInstance()
            .collection("merchants")
            .whereEqualTo("id", code)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot.isEmpty) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val doc    = snapshot.documents.firstOrNull()
                val status = doc?.getString("status")?.let {
                    runCatching { MerchantStatus.valueOf(it) }.getOrNull()
                }
                trySend(
                    if (status == MerchantStatus.DISABLED || status == MerchantStatus.EXPIRED) status
                    else null
                )
            }

        awaitClose { listener.remove() }
    }

    private suspend fun fetchAndStoreAllData(code: String) {
        val data = try {
            firebaseService.fetchAllData(code)
        } catch (e: Exception) {
            return
        }
        data.customers.forEach { c ->
            try { customerDao.insertCustomer(CustomerEntity.fromDomain(c)) } catch (_: Exception) {}
        }
        data.paymentMethods.forEach { m ->
            try { paymentMethodDao.insertPaymentMethod(PaymentMethodEntity.fromDomain(m)) } catch (_: Exception) {}
        }
        data.transactions.forEach { t ->
            try { transactionDao.insertTransaction(TransactionEntity.fromDomain(t)) } catch (_: Exception) {}
        }
        try {
            val products = productFirestoreService.fetchAllProducts(code)
            products.forEach { product ->
                try {
                    val units = productFirestoreService.fetchUnitsForProduct(code, product.id)
                    productDao.insertProduct(product.toEntity())
                    productDao.insertUnits(units.map { it.toEntity() })
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override suspend fun verifyStatusOnStartup(): StartupStatus {
        val activated = isActivated()
        if (!activated) return StartupStatus.NOT_ACTIVATED

        val code = getMerchantCode()
        if (code.isEmpty()) return StartupStatus.NOT_ACTIVATED

        val status = firebaseService.getCodeStatus(code)
            ?: return StartupStatus.OFFLINE

        return when (status) {
            "ACTIVE" -> StartupStatus.ACTIVE
            "DISABLED" -> {
                context.appDataStore.edit { it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = "" }
                StartupStatus.DISABLED
            }
            "EXPIRED" -> {
                context.appDataStore.edit { it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = "" }
                StartupStatus.EXPIRED
            }
            "DELETED" -> {
                context.appDataStore.edit { it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = "" }
                StartupStatus.DELETED
            }
            else -> StartupStatus.ACTIVE
        }
    }

    override suspend fun getMerchantTier(): MerchantTier {
        val raw = context.appDataStore.data.map { it[MERCHANT_TIER] ?: "" }.first()
        return runCatching { MerchantTier.valueOf(raw) }.getOrDefault(MerchantTier.FREE)
    }

    override suspend fun isSelfRegistered(): Boolean =
        context.appDataStore.data.map { it[IS_SELF_REGISTERED] ?: false }.first()

    override suspend fun saveMerchantTier(tier: MerchantTier) {
        context.appDataStore.edit { it[MERCHANT_TIER] = tier.name }
    }

    override suspend fun registerFree(deviceId: String) {
        val merchantId = "free_${deviceId.take(12)}_${System.currentTimeMillis()}"

        FirebaseFirestore.getInstance()
            .collection("merchants")
            .document(merchantId)
            .set(
                mapOf(
                    "id"               to merchantId,
                    "tier"             to MerchantTier.FREE.name,
                    "status"           to "ACTIVE",
                    "isPermanent"      to true,
                    "isSelfRegistered" to true,
                    "activationCode"   to "",
                    "createdAt"        to com.google.firebase.Timestamp.now(),
                    "planName"         to "باقة مجانية",
                    "paymentMethod"    to "تسجيل ذاتي (مجاني)"
                )
            ).await()

        FirebaseDatabase.getInstance()
            .reference
            .child("activation_codes")
            .child(merchantId)
            .setValue(mapOf("status" to "ACTIVE"))
            .await()

        context.appDataStore.edit {
            it[IS_ACTIVATED]       = true
            it[MERCHANT_CODE]      = merchantId
            it[MERCHANT_TIER]      = MerchantTier.FREE.name
            it[IS_SELF_REGISTERED] = true
        }
    }
}
