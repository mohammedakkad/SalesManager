package com.trader.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import com.trader.core.data.local.appDataStore
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
import com.trader.core.domain.model.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    private val cashBoxDao: CashBoxDao
) : ActivationRepository {

    private val realtimeDb = FirebaseDatabase.getInstance().reference

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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMerchantStatus(): Flow<MerchantStatus?> =
        observeMerchantCode()
            .distinctUntilChanged()
            .flatMapLatest { code ->
                if (code.isBlank()) {
                    flowOf(null)
                } else {
                    callbackFlow {
                        val ref = realtimeDb.child(PATH_ACTIVATION_CODES).child(code)
                        val listener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    trySend(null)
                                    return
                                }
                                val statusStr = snapshot.child(FIELD_STATUS).getValue(String::class.java)
                                if (statusStr == null) {
                                    trySend(null)
                                    return
                                }
                                val status = when (statusStr.uppercase()) {
                                    STATUS_ACTIVE -> MerchantStatus.ACTIVE
                                    STATUS_DISABLED -> MerchantStatus.DISABLED
                                    STATUS_EXPIRED -> MerchantStatus.EXPIRED
                                    else -> null
                                }
                                trySend(status)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                        ref.addValueEventListener(listener)
                        awaitClose { ref.removeEventListener(listener) }
                    }
                }
            }

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

        data.invoiceItems.forEach { item ->
            runCatching {
                invoiceItemDao.insertInvoiceItem(
                    InvoiceItemEntity(
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

        data.returns.forEach { (invoice, items) ->
            runCatching {
                returnDao.insertReturnWithItems(
                    invoice.copy(syncStatus = SyncStatus.SYNCED).toEntity(),
                    items.map { it.toEntity() }
                )
            }
        }

        // ✅ الصناديق — تُجلب مرة واحدة عند التفعيل (نفس نمط باقي البيانات)
        data.cashBoxes.forEach { box ->
            runCatching {
                cashBoxDao.upsert(
                    CashBoxEntity.fromDomain(
                        box.copy(merchantId = code, syncStatus = SyncStatus.SYNCED)
                    )
                )
            }
        }

        runCatching { transactionDao.recalculateHasItems() }
    }

    private suspend fun fetchProductsAndUnits(code: String) = runCatching {
        productFirestoreService.fetchAllProducts(code).forEach { product ->
            val units = productFirestoreService.fetchUnitsForProduct(code, product.id)
            productDao.insertProduct(product.toEntity())
            productDao.insertUnits(units.map { it.toEntity() })
        }
    }

    override suspend fun verifyStatusOnStartup(): StartupStatus {
        val code = getMerchantCode()
        if (code.isBlank() || !isActivated()) {
            return StartupStatus.NOT_ACTIVATED
        }

        return when (firebaseService.validateCodeDetailed(code)) {
            ValidationResult.Active -> StartupStatus.ACTIVE
            ValidationResult.Disabled -> StartupStatus.DISABLED
            ValidationResult.Expired -> StartupStatus.EXPIRED
            ValidationResult.NotFound -> StartupStatus.DELETED
            ValidationResult.NetworkError -> StartupStatus.OFFLINE
        }
    }

    override suspend fun isSelfRegistered(): Boolean = false

    override suspend fun registerFree() {
        throw UnsupportedOperationException("Activation requires a code from the administrator")
    }

    companion object {
        private const val PATH_ACTIVATION_CODES = "activation_codes"
        private const val FIELD_STATUS = "status"
        private const val STATUS_ACTIVE = "ACTIVE"
        private const val STATUS_DISABLED = "DISABLED"
        private const val STATUS_EXPIRED = "EXPIRED"

        private val MERCHANT_CODE = stringPreferencesKey("merchant_code")
        private val IS_ACTIVATED = booleanPreferencesKey("is_activated")
    }
}
