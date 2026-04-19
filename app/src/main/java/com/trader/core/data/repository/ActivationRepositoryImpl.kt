package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import com.trader.core.data.local.appDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import com.trader.core.domain.model.StartupStatus
import com.trader.core.data.remote.ValidationResult


class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseSyncService,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val productDao: ProductDao, // ✅ لجلب المنتجات عند التفعيل
    private val productFirestoreService: ProductFirestoreService // ✅
) : ActivationRepository {

    private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
    private val MERCHANT_CODE = stringPreferencesKey("merchant_code")

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

    /**
     * Emits the current merchant code immediately and on every change.
     * Repositories use this to start realtime sync as soon as a code is available —
     * even if the user activates AFTER the repository was created.
     */
    override fun observeMerchantCode(): Flow<String> =
    context.appDataStore.data.map {
        it[MERCHANT_CODE] ?: ""
    }.distinctUntilChanged()


    override suspend fun saveActivationStatus(activated: Boolean, code: String) {
        context.appDataStore.edit {
            it[IS_ACTIVATED] = activated
            it[MERCHANT_CODE] = code
        }
        if (activated && code.isNotEmpty()) {
            fetchAndStoreAllData(code)
        }
    }

    override suspend fun deactivate() {
        context.appDataStore.edit {
            it[IS_ACTIVATED] = false
            it[MERCHANT_CODE] = ""
        }
        customerDao.deleteAll()
        transactionDao.deleteAll()
        paymentMethodDao.deleteAll()
        // ✅ حذف المنتجات المحلية عند إلغاء التفعيل
        productDao.deleteAllProducts()
    }

    /**
     * Watches Firestore for explicit DISABLED/EXPIRED status.
     *
     * KEY FIX: We ONLY emit a non-null status when the document EXISTS and has a
     * recognized status field. An empty Firestore snapshot (merchant not in Firestore,
     * or no internet) emits null — which is treated as "unknown, do nothing".
     * This prevents the previous bug where empty Firestore → DISABLED → deactivate() loop.
     */
    override fun observeMerchantStatus(): Flow<MerchantStatus?> = callbackFlow {
        val code = getMerchantCode()
        if (code.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseFirestore.getInstance()
        .collection("merchants")
        .whereEqualTo("activationCode", code)
        .addSnapshotListener {
            snapshot, error ->
            if (error != null || snapshot == null) {
                // Network error or Firestore not configured → do NOT deactivate
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot.isEmpty) {
                // Merchant document not found in Firestore.
                // Could mean Firestore is not used or doc hasn't been created yet.
                // Emit null = "status unknown" → AppNavigation does nothing.
                trySend(null)
                return@addSnapshotListener
            }
            val doc = snapshot.documents.firstOrNull()
            val status = doc?.getString("status")?.let {
                runCatching {
                    MerchantStatus.valueOf(it)
                }.getOrNull()
            }
            // Only emit DISABLED/EXPIRED — emit null for ACTIVE or unrecognized
            trySend(if (status == MerchantStatus.DISABLED || status == MerchantStatus.EXPIRED) status else null)
        }

        awaitClose {
            listener.remove()
        }
    }

    // ─────────────────────────────────────────────────────────────
    private suspend fun fetchAndStoreAllData(code: String) {
        // ── بيانات العمليات (Realtime DB) ─────────────────────────
        val data = try {
            firebaseService.fetchAllData(code)
        } catch (e: Exception) {
            return
        }
        data.customers.forEach {
            c ->
            try {
                customerDao.insertCustomer(CustomerEntity.fromDomain(c))
            } catch (_: Exception) {}
        }
        data.paymentMethods.forEach {
            m ->
            try {
                paymentMethodDao.insertPaymentMethod(PaymentMethodEntity.fromDomain(m))
            } catch (_: Exception) {}
        }
        data.transactions.forEach {
            t ->
            try {
                transactionDao.insertTransaction(TransactionEntity.fromDomain(t))
            } catch (_: Exception) {}
        }

        // ✅ المنتجات والوحدات من subcollection الجديدة
        try {
            val products = productFirestoreService.fetchAllProducts(code)
            products.forEach {
                product ->
                try {
                    // جلب وحدات كل منتج من subcollection خاصة به
                    val units = productFirestoreService.fetchUnitsForProduct(code, product.id)
                    productDao.insertProduct(product.toEntity())
                    productDao.insertUnits(units.map {
                        it.toEntity()
                    })
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override suspend fun verifyStatusOnStartup(): StartupStatus {
        val activated = isActivated()
        if (!activated) return StartupStatus.NOT_ACTIVATED

        val code = getMerchantCode()
        if (code.isEmpty()) return StartupStatus.NOT_ACTIVATED

        // Check Firebase status
        val status = firebaseService.getCodeStatus(code)
        ?: return StartupStatus.OFFLINE // offline — allow entry

        return when (status) {
            "ACTIVE" -> StartupStatus.ACTIVE
            "DISABLED" -> {
                // Auto-clear local activation
                context.appDataStore.edit {
                    it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = ""
                }
                StartupStatus.DISABLED
            }
            "EXPIRED" -> {
                context.appDataStore.edit {
                    it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = ""
                }
                StartupStatus.EXPIRED
            }
            "DELETED" -> {
                context.appDataStore.edit {
                    it[IS_ACTIVATED] = false; it[MERCHANT_CODE] = ""
                }
                StartupStatus.DELETED
            } else -> StartupStatus.ACTIVE
        }
    }
}