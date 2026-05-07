package com.trader.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.*
import com.trader.core.data.local.appDataStore
import com.google.firebase.firestore.DocumentSnapshot
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val productDao: ProductDao,
    private val productFirestoreService: ProductFirestoreService,
    private val returnDao: ReturnDao // ✅ 1. تمت إضافة ReturnDao هنا
) : ActivationRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun observeMerchantTier(): Flow<MerchantTier> =
    context.appDataStore.data
    .map {
        it[KEY_TIER] ?: MerchantTier.FREE.name
    }
    .map {
        parseTier(it)
    }
    .distinctUntilChanged()

    override suspend fun validateCode(code: String) = firebaseService.validateCode(code)

    override suspend fun validateCodeDetailed(code: String): ValidationResult =
    firebaseService.validateCodeDetailed(code)

    override suspend fun isActivated() =
    context.appDataStore.data.map {
        it[IS_ACTIVATED] ?: false
    }.first()

    override suspend fun getMerchantCode() =
    context.appDataStore.data.map {
        it[MERCHANT_CODE] ?: ""
    }.first()

    override fun observeMerchantCode(): Flow<String> =
    context.appDataStore.data.map {
        it[MERCHANT_CODE] ?: ""
    }.distinctUntilChanged()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        val deviceId = getHardwareId()
        context.appDataStore.edit {
            it[IS_ACTIVATED] = activated
            it[MERCHANT_CODE] = code
        }
        if (activated && code.isNotEmpty()) {
            bindDeviceToCode(code, deviceId)
            fetchAndStoreAllData(code)
        }
    }

    private suspend fun bindDeviceToCode(code: String, deviceId: String) {
        firestore.collection(COLLECTION_MERCHANTS)
        .document(code)
        .update(FIELD_DEVICE_ID, deviceId)
        .await()
    }

    override suspend fun deactivate() {
        context.appDataStore.edit {
            it[IS_ACTIVATED] = false
            it[MERCHANT_CODE] = ""
        }
        customerDao.deleteAll()
        transactionDao.deleteAll()
        paymentMethodDao.deleteAll()
        productDao.deleteAllProducts()
    }

    override fun observeMerchantStatus(): Flow<MerchantStatus?> = callbackFlow {
        val id = getHardwareId()
        val listener = firestore.collection(COLLECTION_MERCHANTS).document(id)
        .addSnapshotListener {
            snap, _ ->
            snap?.let {
                repositoryScope.launch {
                    saveMerchantTier(parseTier(it.getString(FIELD_TIER)))
                }
                trySend(parseStatus(it.getString(FIELD_STATUS)))
            }
        }
        awaitClose {
            listener.remove()
        }
    }

    private fun parseTier(raw: String?) =
    runCatching {
        MerchantTier.valueOf(raw!!)
    }.getOrDefault(MerchantTier.FREE)

    private fun parseStatus(raw: String?) =
    runCatching {
        MerchantStatus.valueOf(raw!!)
    }.getOrNull()

    private suspend fun fetchAndStoreAllData(code: String) {
        val data = try {
            firebaseService.fetchAllData(code)
        } catch (e: Exception) {
            return
        }

        data.customers.forEach {
            runCatching {
                customerDao.insertCustomer(CustomerEntity.fromDomain(it))
            }
        }
        data.paymentMethods.forEach {
            runCatching {
                paymentMethodDao.insertPaymentMethod(PaymentMethodEntity.fromDomain(it))
            }
        }
        data.transactions.forEach {
            runCatching {
                transactionDao.insertTransaction(TransactionEntity.fromDomain(it))
            }
        }

        // ✅ 2. جلب المرتجعات وحفظها في التخزين المحلي (Room) بأمان
        data.returns.forEach {
            (invoice, items) ->
            runCatching {
                returnDao.insertReturnInvoice(invoice.toEntity())
                returnDao.insertReturnItems(items.map {
                    it.toEntity()
                })
            }
        }

        fetchProductsAndUnits(code)
    }

    private suspend fun fetchProductsAndUnits(code: String) = runCatching {
        productFirestoreService.fetchAllProducts(code).forEach {
            product ->
            val units = productFirestoreService.fetchUnitsForProduct(code, product.id)
            productDao.insertProduct(product.toEntity())
            productDao.insertUnits(units.map {
                it.toEntity()
            })
        }
    }

    override suspend fun verifyStatusOnStartup(): StartupStatus {
        val deviceId = getHardwareId()
        return try {
            val doc = findMerchantDocument(deviceId)
            if (doc != null) restoreSession(doc) else StartupStatus.NOT_ACTIVATED
        } catch (e: Exception) {
            StartupStatus.OFFLINE
        }
    }

    private suspend fun findMerchantDocument(deviceId: String): DocumentSnapshot? {
        val directDoc = firestore.collection(COLLECTION_MERCHANTS).document(deviceId).get().await()
        if (directDoc.exists()) return directDoc

        return firestore.collection(COLLECTION_MERCHANTS)
        .whereEqualTo(FIELD_DEVICE_ID, deviceId)
        .limit(1)
        .get()
        .await()
        .documents
        .firstOrNull()
    }

    override suspend fun getMerchantTier(): MerchantTier = parseTier(
        context.appDataStore.data.map {
            it[MERCHANT_TIER]
        }.first()
    )

    override suspend fun isSelfRegistered(): Boolean =
    context.appDataStore.data.map {
        it[IS_SELF_REGISTERED] ?: false
    }.first()

    override suspend fun saveMerchantTier(tier: MerchantTier) {
        context.appDataStore.edit {
            it[MERCHANT_TIER] = tier.name
        }
    }

    override suspend fun registerFree() {
        val id = getHardwareId()
        val merchantData = mapOf(
            FIELD_ID to id,
            FIELD_DEVICE_ID to id,
            FIELD_TIER to MerchantTier.FREE.name,
            FIELD_STATUS to STATUS_ACTIVE,
            FIELD_IS_SELF_REG to true,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection(COLLECTION_MERCHANTS).document(id).set(merchantData).await()
        saveLocalSession(id, MerchantTier.FREE.name, true, true)
    }

    @SuppressLint("HardwareIds")
    private fun getHardwareId(): String = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ANDROID_ID
    ) ?: "unknown_device"

    private suspend fun restoreSession(doc: DocumentSnapshot): StartupStatus {
        val status = doc.getString(FIELD_STATUS) ?: STATUS_ACTIVE
        val tier = doc.getString(FIELD_TIER) ?: MerchantTier.FREE.name
        val isSelfReg = doc.getBoolean(FIELD_IS_SELF_REG) ?: false

        saveLocalSession(doc.id, tier, status == STATUS_ACTIVE, isSelfReg)
        return mapToStartupStatus(status)
    }

    private suspend fun saveLocalSession(code: String, tier: String, active: Boolean, self: Boolean) {
        context.appDataStore.edit {
            it[KEY_CODE] = code
            it[KEY_TIER] = tier
            it[KEY_ACTIVATED] = active
            it[IS_SELF_REGISTERED] = self
        }
    }

    private fun mapToStartupStatus(status: String) = when (status) {
        STATUS_ACTIVE -> StartupStatus.ACTIVE
        STATUS_DISABLED -> StartupStatus.DISABLED
        else -> StartupStatus.EXPIRED
    }

    companion object {
        private const val COLLECTION_MERCHANTS = "merchants"
        private const val FIELD_STATUS = "status"
        private const val FIELD_TIER = "tier"
        private const val FIELD_ID = "id"
        private const val FIELD_DEVICE_ID = "deviceId"
        private const val FIELD_IS_SELF_REG = "isSelfRegistered"
        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_DISABLED = "DISABLED"

        private val MERCHANT_TIER = stringPreferencesKey("merchant_tier")
        private val MERCHANT_CODE = stringPreferencesKey("merchant_code")
        private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
        private val IS_SELF_REGISTERED = booleanPreferencesKey("is_self_registered")
        private val KEY_TIER = stringPreferencesKey("merchant_tier")
        private val KEY_CODE = stringPreferencesKey("merchant_code")
        private val KEY_ACTIVATED = booleanPreferencesKey("is_activated")
    }
}