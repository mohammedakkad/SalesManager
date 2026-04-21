package com.trader.core.data.repository

import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.trader.core.domain.model.SyncStatus


class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val stockRepo: StockRepository
) : TransactionRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    /**
     * FIX: Watch for merchant code via Flow instead of reading once.
     * Previously: read code once in init → empty on fresh install → sync never starts.
     * Now: waits until a non-empty code is emitted (happens right after activation),
     *      then starts the realtime Firebase listener.
     * If the user deactivates (code → ""), the sync collector is cancelled automatically.
     */
    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
            .filter {
                it.isNotEmpty()
            } // wait for activation
            .distinctUntilChanged()
            .collectLatest {
                code ->
                // collectLatest cancels previous collector when code changes
                sync.observeTransactions(code).collect {
                    list ->
                    list.forEach {
                        t ->
                        try {
                            transactionDao.insertTransaction(TransactionEntity.fromDomain(t))
                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                            // Customer not synced yet — skip silently
                        }
                    }
                }
            }
        }
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    private suspend fun TransactionEntity.enrich() = toDomain(
        customerName = customerDao.getCustomerById(customerId)?.name ?: "",
        paymentMethodName = paymentMethodId?.let {
            paymentMethodDao.getPaymentMethodById(it)?.name
        } ?: ""
    )

    override fun getAllTransactions(): Flow<List<Transaction>> =
    transactionDao.getAllTransactions().map {
        it.map {
            e -> e.enrich()
        }
    }

    override fun getTransactionsByCustomer(cid: Long) =
    transactionDao.getTransactionsByCustomer(cid).map {
        it.map {
            e -> e.enrich()
        }
    }

    override fun getTransactionsByDate(s: Long, e: Long) =
    transactionDao.getTransactionsByDate(s, e).map {
        it.map {
            en -> en.enrich()
        }
    }

    override fun getUnpaidTransactions() =
    transactionDao.getUnpaidTransactions().map {
        it.map {
            e -> e.enrich()
        }
    }

    override suspend fun getTransactionById(id: Long) = transactionDao.getTransactionById(id)?.enrich()

    override suspend fun insertTransaction(t: Transaction): Long {
        // ✅ يُحفظ كـ PENDING أولاً
        val entity = TransactionEntity.fromDomain(t.copy(syncStatus = SyncStatus.PENDING))
        val id = transactionDao.insertTransaction(entity)
        syncScope.launch {
            try {
                sync.pushTransaction(code(), t.copy(id = id))
                transactionDao.markSynced(id) // ✅ يصبح SYNCED بعد الرفع
            } catch (_: Exception) {}
        }
        return id
    }

    override suspend fun updateTransaction(t: Transaction) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(t))
        syncScope.launch {
            try {
                sync.pushTransaction(code(), t)
            } catch (_: Exception) {}
        }
    }

    override suspend fun deleteTransaction(t: Transaction) {
        // ✅ إرجاع المخزون إذا كانت العملية تحتوي أصناف
        if (t.hasItems) {
            val items = invoiceItemRepo.getItemsForTransactionOnce(t.id)
            items.forEach {
                item ->
                stockRepo.returnStock(
                    productId = item.productId,
                    unitId = item.unitId,
                    quantity = item.quantity,
                    transactionId = t.id,
                    productName = item.productName,
                    unitLabel = item.unitLabel
                )
            }
            invoiceItemRepo.deleteItemsForTransaction(t.id)
        }
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(t))
        syncScope.launch {
            try {
                sync.deleteTransaction(code(), t.id)
            } catch (_: Exception) {}
        }
    }

    override suspend fun getTotalAmountByDate(s: Long, e: Long) = transactionDao.getTotalAmountByDate(s, e)
    override suspend fun getPaidAmountByDate(s: Long, e: Long) = transactionDao.getPaidAmountByDate(s, e)
    override suspend fun getUnpaidAmountByCustomer(cid: Long) = transactionDao.getUnpaidAmountByCustomer(cid)
}