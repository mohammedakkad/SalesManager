package com.trader.core.data.repository

import android.annotation.SuppressLint
import android.os.Build
import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.*
import com.trader.core.data.local.appDataStore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.model.MerchantTier
import com.trader.core.domain.model.Session
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult
import com.trader.core.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val productDao: ProductDao,
    private val productFirestoreService: ProductFirestoreService,
    private val returnDao: ReturnDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val sessionDao: SessionDao
) : ActivationRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

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

    override fun observeCurrentSessionId(): Flow<String> =
    context.appDataStore.data.map {
        it[CURRENT_SESSION_ID] ?: ""
    }.distinctUntilChanged()

    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.appDataStore.edit {
            it[IS_ACTIVATED] = activated
            it[MERCHANT_CODE] = code
        }
        if (activated && code.isNotEmpty()) {
            runCatching {
                registerCurrentSession(code)
            }
            fetchAndStoreAllData(code)
        }
    }

    override suspend fun deactivate() {
        context.appDataStore.edit {
            it[IS_ACTIVATED] = false
            it[MERCHANT_CODE] = ""
            it[CURRENT_SESSION_ID] = ""
        }
        customerDao.deleteAll()
        transactionDao.deleteAll()
        paymentMethodDao.deleteAll()
        productDao.deleteAllProducts()
        sessionDao.deleteAll()
    }

    /**
     * Observes merchant status from Firestore in real-time.
     *
     * Key rules to prevent false-positive "Account Blocked" dialogs:
     *  1. FREE users (isSelfRegistered = true) are looked up by HardwareID,
     *     not by merchantCode alone, because their document may be keyed on deviceId.
     *  2. A null / missing-document result is only emitted if the server
     *     CONFIRMED the document existed at least once and it has now been removed
     *     (i.e. we skip cache-only snapshots for deletion signals).
     *  3. Network errors are silently ignored — the last known status is kept.
     *  4. A status of BLOCKED (which maps to null in this repo) is only emitted
     *     when the document explicitly carries status = DISABLED/EXPIRED, never
     *     while still connecting.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMerchantStatus(): Flow<MerchantStatus?> =
        context.appDataStore.data
            .map { prefs ->
                val code = prefs[MERCHANT_CODE] ?: ""
                val isFree = prefs[IS_SELF_REGISTERED] ?: false
                code to isFree
            }
            .distinctUntilChanged()
            .flatMapLatest { (merchantCode, isFree) ->
                if (merchantCode.isBlank()) {
                    flowOf(null)
                } else {
                    callbackFlow {
                        // For FREE users the document may be keyed on the device's ANDROID_ID,
                        // so we resolve the actual document reference first.
                        val docRef = if (isFree) {
                            val deviceId = getHardwareId()
                            // Prefer exact match by deviceId document; fall back to merchantCode.
                            val byDevice = runCatching {
                                firestore.collection(COLLECTION_MERCHANTS).document(deviceId).get().await()
                            }.getOrNull()
                            if (byDevice?.exists() == true) {
                                firestore.collection(COLLECTION_MERCHANTS).document(deviceId)
                            } else {
                                firestore.collection(COLLECTION_MERCHANTS).document(merchantCode)
                            }
                        } else {
                            firestore.collection(COLLECTION_MERCHANTS).document(merchantCode)
                        }

                        // Track whether we have ever received a confirmed server-side existence
                        // so we don't emit "deleted" on first-run cache misses or while offline.
                        var documentConfirmedExistOnServer = false

                        val listener = docRef.addSnapshotListener { snap, error ->
                            // Network errors — keep last known state, do not emit anything.
                            if (error != null) return@addSnapshotListener

                            when {
                                snap == null -> return@addSnapshotListener

                                snap.exists() -> {
                                    // Document is present — update tier and forward status.
                                    if (!snap.metadata.isFromCache) {
                                        documentConfirmedExistOnServer = true
                                    }
                                    repositoryScope.launch {
                                        saveMerchantTier(parseTier(snap.getString(FIELD_TIER)))
                                    }
                                    val rawStatus = snap.getString(FIELD_STATUS)
                                    // Only propagate DISABLED / EXPIRED; treat ACTIVE (and anything
                                    // else, including a missing field) as healthy.
                                    val status = when (rawStatus?.uppercase()) {
                                        STATUS_DISABLED, "BLOCKED" -> MerchantStatus.DISABLED
                                        "EXPIRED" -> MerchantStatus.EXPIRED
                                        else -> MerchantStatus.ACTIVE
                                    }
                                    trySend(status)
                                }

                                !snap.exists() && !snap.metadata.isFromCache -> {
                                    // Server confirmed document does not exist.
                                    // Only treat as "deleted/blocked" if we previously saw it exist
                                    // on the server; otherwise this is just a new/unregistered user.
                                    if (documentConfirmedExistOnServer) {
                                        trySend(null)
                                    }
                                    // else: document never confirmed — stay silent (don't block user).
                                }

                                // snap exists only in cache, or isFromCache + not exists → ignore.
                            }
                        }

                        awaitClose { listener.remove() }
                    }
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
        // ✅ Fix 2: بناء Set من IDs العمليات التي لها invoice_items فعلية في Firebase
        // يضمن صحة hasItems حتى للبيانات القديمة التي لم تُخزَّن hasItems من قبل
        val transactionIdsWithItems = data.invoiceItems
            .map { it.transactionId }
            .toSet()

        data.transactions.forEach { transaction ->
            runCatching {
                val correctedTransaction = transaction.copy(
                    hasItems = transaction.id in transactionIdsWithItems
                )
                transactionDao.insertTransaction(TransactionEntity.fromDomain(correctedTransaction))
            }
        }

        fetchProductsAndUnits(code)

        data.invoiceItems.forEach {
            item ->
            runCatching {
                invoiceItemDao.insertInvoiceItem(
                    com.trader.core.data.local.entity.InvoiceItemEntity(
                        id = item.id,
                        transactionId = item.transactionId,
                        productId = item.productId,
                        productName = item.productName,
                        unitId = item.unitId,
                        unitLabel = item.unitLabel,
                        quantity = item.quantity,
                        pricePerUnit = item.pricePerUnit,
                        totalPrice = item.totalPrice,
                        merchantId = code,
                        syncStatus = "SYNCED"
                    )
                )
            }
        }

        data.returns.forEach {
            (invoice, items) ->
            runCatching {
                returnDao.insertReturnWithItems(
                    invoice.copy(syncStatus = SyncStatus.SYNCED).toEntity(),
                    items.map {
                        it.toEntity()
                    }
                )
            }
        }

        // ✅ Fix 2 (Self-Healing): تصحيح أي بيانات قديمة في DB قيمة hasItems فيها خاطئة
        runCatching { transactionDao.recalculateHasItems() }
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

    // ✅ إصلاح خلل الحظر عند انقطاع الإنترنت
    override suspend fun verifyStatusOnStartup(): StartupStatus {
        val deviceId = getHardwareId()
        val localCode = getMerchantCode()
        val locallyActivated = isActivated() // فحص هل هو مسجل محلياً

        return try {
            val doc = findMerchantDocument(localCode, deviceId)
            if (doc != null && doc.exists()) {
                val startupStatus = restoreSession(doc)
                if (startupStatus == StartupStatus.ACTIVE) {
                    enqueueCurrentSessionRegistration(doc.id)
                }
                startupStatus
            } else if (locallyActivated && localCode.isNotBlank()) {
                // 🚀 هجرة صامتة للمستخدمين القدامى: تسجيل جلسة جديدة دون أي تدخل
                enqueueCurrentSessionRegistration(localCode)
                StartupStatus.ACTIVE
            } else if (locallyActivated) {
                StartupStatus.ACTIVE
            } else {
                StartupStatus.NOT_ACTIVATED
            }
        } catch (e: Exception) {
            // 🚀 إذا فشل الاتصال (أوفلاين)، دعه يدخل إذا كان مسجلاً مسبقاً
            if (locallyActivated) {
                if (localCode.isNotBlank()) {
                    enqueueCurrentSessionRegistration(localCode)
                }
                StartupStatus.ACTIVE
            } else {
                StartupStatus.OFFLINE
            }
        }
    }

    private suspend fun findMerchantDocument(merchantCode: String, deviceId: String): DocumentSnapshot? {
        if (merchantCode.isNotBlank()) {
            val byCode = runCatching {
                firestore.collection(COLLECTION_MERCHANTS).document(merchantCode).get().await()
            }.getOrNull()
            if (byCode?.exists() == true) return byCode
        }

        val directDoc = runCatching {
            firestore.collection(COLLECTION_MERCHANTS).document(deviceId).get().await()
        }.getOrNull()
        if (directDoc?.exists() == true) return directDoc

        return runCatching {
            firestore.collection(COLLECTION_MERCHANTS)
            .whereEqualTo(FIELD_DEVICE_ID, deviceId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
        }.getOrNull()
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
        val deviceId = getHardwareId()
        val merchantCode = generateFreeMerchantCode()
        val merchantData = mapOf(
            FIELD_ID to merchantCode,
            FIELD_DEVICE_ID to deviceId,
            FIELD_TIER to MerchantTier.FREE.name,
            FIELD_STATUS to STATUS_ACTIVE,
            FIELD_IS_SELF_REG to true,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        runCatching {
            firestore.collection(COLLECTION_MERCHANTS).document(merchantCode).set(merchantData).await()
        }.getOrElse {
            throw it
        }
        saveLocalSession(merchantCode, MerchantTier.FREE.name, true, true)
        runCatching {
            registerCurrentSession(merchantCode)
        }
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

    private suspend fun registerCurrentSession(
        merchantCode: String,
        onlyIfMissing: Boolean = false
    ) {
        if (merchantCode.isBlank()) return

        val session = buildCurrentSession()
        val sessionRef = realtimeDb
            .child(COLLECTION_MERCHANTS)
            .child(merchantCode)
            .child(PATH_SESSIONS)
            .child(session.id)

        runCatching {
            if (onlyIfMissing) {
                val exists = sessionRef.get().await().exists()
                if (exists) {
                    sessionRef.child(FIELD_LAST_ACTIVE).setValue(session.lastActive).await()
                } else {
                    sessionRef.setValue(session.toFirebaseMap()).await()
                }
            } else {
                sessionRef.setValue(session.toFirebaseMap()).await()
            }
        }
    }

    private fun enqueueCurrentSessionRegistration(merchantCode: String) {
        if (merchantCode.isBlank()) return
        repositoryScope.launch {
            runCatching {
                registerCurrentSession(merchantCode, onlyIfMissing = true)
            }
        }
    }

    private suspend fun buildCurrentSession(): Session {
        val now = System.currentTimeMillis()
        val sessionId = getOrCreateSessionId()
        val cached = sessionDao.getById(sessionId)

        val session = Session(
            id = sessionId,
            deviceId = getHardwareId(),
            deviceName = Build.MODEL ?: "Unknown Device",
            loginDate = cached?.loginDate ?: now,
            lastActive = now
        )
        sessionDao.upsertAtomic(session.toEntity())
        return session
    }

    private suspend fun getOrCreateSessionId(): String {
        val cached = context.appDataStore.data.map {
            it[CURRENT_SESSION_ID] ?: ""
        }.first()
        if (cached.isNotBlank()) return cached

        val newSessionId = UUID.randomUUID().toString()
        context.appDataStore.edit {
            it[CURRENT_SESSION_ID] = newSessionId
        }
        return newSessionId
    }

    private suspend fun generateFreeMerchantCode(): String {
        repeat(5) {
            val candidate = "FREE-${randomSuffix(8)}"
            val exists = runCatching {
                firestore.collection(COLLECTION_MERCHANTS).document(candidate).get().await().exists()
            }.getOrDefault(false)
            if (!exists) return candidate
        }
        return "FREE-${randomSuffix(8)}"
    }

    private fun randomSuffix(length: Int): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..length).joinToString("") {
            chars.random().toString()
        }
    }

    private fun Session.toFirebaseMap(): Map<String, Any> = mapOf(
        FIELD_ID to id,
        FIELD_DEVICE_ID to deviceId,
        FIELD_DEVICE_NAME to deviceName,
        FIELD_LOGIN_DATE to loginDate,
        FIELD_LAST_ACTIVE to lastActive
    )

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
        private const val FIELD_DEVICE_NAME = "deviceName"
        private const val FIELD_LOGIN_DATE = "loginDate"
        private const val FIELD_LAST_ACTIVE = "lastActive"
        private const val FIELD_IS_SELF_REG = "isSelfRegistered"
        private const val PATH_SESSIONS = "sessions"
        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_DISABLED = "DISABLED"

        private val MERCHANT_TIER = stringPreferencesKey("merchant_tier")
        private val MERCHANT_CODE = stringPreferencesKey("merchant_code")
        private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
        private val IS_SELF_REGISTERED = booleanPreferencesKey("is_self_registered")
        private val CURRENT_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_TIER = stringPreferencesKey("merchant_tier")
        private val KEY_CODE = stringPreferencesKey("merchant_code")
        private val KEY_ACTIVATED = booleanPreferencesKey("is_activated")
    }
}