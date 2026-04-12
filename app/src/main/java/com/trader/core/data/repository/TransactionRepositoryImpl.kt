package com.trader.core.data.repository

import com.trader.core.data.local.dao.*
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : TransactionRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init { startRealtimeSync() }

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
                .filter { it.isNotEmpty() }  // wait for activation
                .distinctUntilChanged()
                .collectLatest { code ->
                    // collectLatest cancels previous collector when code changes
                    sync.observeTransactions(code).collect { list ->
                        list.forEach { transactionDao.insertTransaction(TransactionEntity.fromDomain(it)) }
                    }
                }
        }
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    private suspend fun TransactionEntity.enrich() = toDomain(
        customerName      = customerDao.getCustomerById(customerId)?.name ?: "",
        paymentMethodName = paymentMethodId?.let { paymentMethodDao.getPaymentMethodById(it)?.name } ?: ""
    )

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { it.map { e -> e.enrich() } }

    override fun getTransactionsByCustomer(cid: Long) =
        transactionDao.getTransactionsByCustomer(cid).map { it.map { e -> e.enrich() } }

    override fun getTransactionsByDate(s: Long, e: Long) =
        transactionDao.getTransactionsByDate(s, e).map { it.map { en -> en.enrich() } }

    override fun getUnpaidTransactions() =
        transactionDao.getUnpaidTransactions().map { it.map { e -> e.enrich() } }

    override suspend fun getTransactionById(id: Long) = transactionDao.getTransactionById(id)?.enrich()

    override suspend fun insertTransaction(t: Transaction): Long {
        val id = transactionDao.insertTransaction(TransactionEntity.fromDomain(t))
        sync.pushTransaction(code(), t.copy(id = id))
        return id
    }

    override suspend fun updateTransaction(t: Transaction) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(t))
        sync.pushTransaction(code(), t)
    }

    override suspend fun deleteTransaction(t: Transaction) {
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(t))
        sync.deleteTransaction(code(), t.id)
    }

    override suspend fun getTotalAmountByDate(s: Long, e: Long) = transactionDao.getTotalAmountByDate(s, e)
    override suspend fun getPaidAmountByDate(s: Long, e: Long)  = transactionDao.getPaidAmountByDate(s, e)
    override suspend fun getUnpaidAmountByCustomer(cid: Long)   = transactionDao.getUnpaidAmountByCustomer(cid)
}
