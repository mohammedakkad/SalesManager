package com.trader.salesmanager.data.repository

import com.trader.salesmanager.data.local.dao.CustomerDao
import com.trader.salesmanager.data.local.dao.PaymentMethodDao
import com.trader.salesmanager.data.local.dao.TransactionDao
import com.trader.salesmanager.data.local.entity.TransactionEntity
import com.trader.salesmanager.data.remote.FirebaseSyncService
import com.trader.salesmanager.domain.model.Transaction
import com.trader.salesmanager.domain.repository.ActivationRepository
import com.trader.salesmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val syncService: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : TransactionRepository {

    private suspend fun merchantCode() = activationRepo.getMerchantCode()

    private suspend fun TransactionEntity.toEnrichedDomain(): Transaction {
        val customerName      = customerDao.getCustomerById(customerId)?.name ?: ""
        val paymentMethodName = paymentMethodId?.let { paymentMethodDao.getPaymentMethodById(it)?.name } ?: ""
        return toDomain(customerName, paymentMethodName)
    }

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { list -> list.map { it.toEnrichedDomain() } }

    override fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCustomer(customerId).map { list -> list.map { it.toEnrichedDomain() } }

    override fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDate(startDate, endDate).map { list -> list.map { it.toEnrichedDomain() } }

    override fun getUnpaidTransactions(): Flow<List<Transaction>> =
        transactionDao.getUnpaidTransactions().map { list -> list.map { it.toEnrichedDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)?.toEnrichedDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
        val saved = transaction.copy(id = id)
        syncService.pushTransaction(merchantCode(), saved)   // push to Firebase
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
        syncService.pushTransaction(merchantCode(), transaction) // push to Firebase
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(transaction))
        syncService.deleteTransaction(merchantCode(), transaction.id) // delete from Firebase
    }

    override suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double =
        transactionDao.getTotalAmountByDate(startDate, endDate)

    override suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double =
        transactionDao.getPaidAmountByDate(startDate, endDate)

    override suspend fun getUnpaidAmountByCustomer(customerId: Long): Double =
        transactionDao.getUnpaidAmountByCustomer(customerId)
}
