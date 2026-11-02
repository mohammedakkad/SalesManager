package com.trader.core.data.repository

import androidx.room.withTransaction
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CashBoxRepository
import com.trader.core.domain.repository.InvoiceItemRepository
import com.trader.core.domain.repository.StockRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.trader.core.domain.model.SyncStatus
import kotlinx.coroutines.delay


class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository,
    private val invoiceItemRepo: InvoiceItemRepository,
    private val stockRepo: StockRepository,
    private val cashBoxRepo: CashBoxRepository,
    private val returnDao: ReturnDao,
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val outboxDatabase: com.trader.core.data.local.db.AppDatabase? = null
) : TransactionRepository {


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
    private suspend fun localFence(body: suspend () -> Unit) {
        if (outboxDatabase == null) body() else outboxDatabase.withTransaction { body() }
    }

    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode().distinctUntilChanged().collectLatest { merchant ->
                if (merchant.isBlank()) return@collectLatest
                sync.observeTransactions(merchant).collect { list ->
                    list.forEach { remote -> localFence {
                        if (outboxDatabase?.saleOutboxDao()?.manages(remote.id) != true) {
                            val existing = transactionDao.getTransactionById(remote.id)
                            if (existing == null || existing.syncStatus == SyncStatus.SYNCED.name) {
                                try { transactionDao.insertTransaction(TransactionEntity.fromDomain(remote.copy(syncStatus = SyncStatus.SYNCED))) }
                                catch (missingParent: android.database.sqlite.SQLiteConstraintException) { /* A later customer snapshot can resolve this. */ }
                            }
                        }
                    } }
                    val remoteIds = list.map { it.id }.toSet()
                    transactionDao.getAllIds().filter { it !in remoteIds }.forEach { id -> localFence {
                        if (outboxDatabase?.saleOutboxDao()?.manages(id) != true) {
                            val local = transactionDao.getTransactionById(id)
                            if (local?.syncStatus == SyncStatus.SYNCED.name) {
                                if (outboxDatabase != null) transactionDao.markConflicting(id)
                                else deleteTransactionLocally(local.enrich(), syncRemote = false)
                            }
                        }
                    } }
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

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
    transactionDao.observeRecentTransactions(limit).map {
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

    // ✅ مشكلة 5: Flow مستمر بدلاً من قراءة آنية — يُحدِّث شاشة التفاصيل فوراً عند أي تغيير
    override fun observeTransactionById(id: Long): Flow<Transaction?> =
    transactionDao.observeTransactionById(id).map {
        it?.enrich()
    }

    override suspend fun insertTransaction(t: Transaction): Long {
        val entity = TransactionEntity.fromDomain(t.copy(syncStatus = SyncStatus.PENDING))
        val id = transactionDao.insertTransaction(entity)
        cashBoxRepo.applyTransactionEffect(
            relatedTransactionId = id,
            oldPaymentMethodId = null, oldAmount = 0.0, oldWasPaid = false,
            newPaymentMethodId = t.paymentMethodId, newAmount = t.amount, newWasPaid = t.isPaid
        )
        if (outboxDatabase?.inTransaction() != true) syncScope.launch {
            if (outboxDatabase?.saleOutboxDao()?.manages(id) == true) return@launch
            runCatching { sync.pushTransaction(code(), t.copy(id = id)) }
                .onSuccess { transactionDao.markSynced(id) }
                .onFailure { transactionDao.markFailed(id) }
        }
        return id
    }

    override suspend fun updateTransaction(t: Transaction) {
        val old = transactionDao.getTransactionById(t.id)
        transactionDao.updateTransaction(
            TransactionEntity.fromDomain(t.copy(syncStatus = SyncStatus.PENDING))
        )
        cashBoxRepo.applyTransactionEffect(
            relatedTransactionId = t.id,
            oldPaymentMethodId = old?.paymentMethodId,
            oldAmount = old?.amount ?: 0.0,
            oldWasPaid = old?.isPaid ?: false,
            newPaymentMethodId = t.paymentMethodId,
            newAmount = t.amount,
            newWasPaid = t.isPaid
        )
        if (outboxDatabase?.inTransaction() != true) syncScope.launch {
            if (outboxDatabase?.saleOutboxDao()?.manages(t.id) == true) return@launch
            runCatching {
                sync.pushTransaction(code(), t)
                delay(600)
            }
                .onSuccess { transactionDao.markSynced(t.id) }
                .onFailure { transactionDao.markFailed(t.id) }
        }
    }

    override suspend fun syncPendingTransactions() {
        val merchantCode = code()
        if (merchantCode.isEmpty()) return
        transactionDao.getAllOnce()
            .filter { it.syncStatus == SyncStatus.PENDING.name || it.syncStatus == SyncStatus.FAILED.name }
            .forEach { entity ->
                if (outboxDatabase?.saleOutboxDao()?.manages(entity.id) == true) return@forEach
                runCatching { sync.pushTransaction(merchantCode, entity.enrich()) }
                    .onSuccess { transactionDao.finishUpload(entity, true) }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        transactionDao.finishUpload(entity, false)
                    }
            }
    }

    override suspend fun deleteTransaction(t: Transaction) {
        deleteTransactionLocally(t, syncRemote = true)
    }

    private suspend fun deleteTransactionLocally(t: Transaction, syncRemote: Boolean) {
        val items = invoiceItemRepo.getItemsForTransactionOnce(t.id)
        val returnedByUnit = returnDao.getReturnedByUnit(t.id).associate { it.unitId to it.total }
        items.groupBy { it.unitId }.forEach { (unitId, lines) ->
            val remaining = (lines.sumOf { it.quantity } - (returnedByUnit[unitId] ?: 0.0)).coerceAtLeast(0.0)
            if (remaining > 0) {
                val item = lines.first()
                stockRepo.returnStock(item.productId, unitId, remaining, t.id, item.productName, item.unitLabel)
            }
        }
        returnDao.deleteForTransaction(t.id)
        invoiceItemRepo.deleteItemsForTransaction(t.id)
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(t))
        cashBoxRepo.applyTransactionEffect(
            relatedTransactionId = t.id,
            oldPaymentMethodId = t.paymentMethodId, oldAmount = t.amount, oldWasPaid = t.isPaid,
            newPaymentMethodId = null, newAmount = 0.0, newWasPaid = false
        )
        if (syncRemote) {
            syncScope.launch {
                try {
                    sync.deleteTransaction(code(), t.id)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun getTotalAmountByDate(s: Long, e: Long) = transactionDao.getTotalAmountByDate(s, e)
    override suspend fun getPaidAmountByDate(s: Long, e: Long) = transactionDao.getPaidAmountByDate(s, e)
    override suspend fun getUnpaidAmountByCustomer(cid: Long) = transactionDao.getUnpaidAmountByCustomer(cid)
    override suspend fun getUnpaidAmountBeforeTransaction(
        customerId: Long,
        transactionDate: Long,
        transactionId: Long
    ) = transactionDao.getUnpaidAmountBeforeTransaction(customerId, transactionDate, transactionId)
}
