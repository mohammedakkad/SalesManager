package com.trader.core.data.repository

import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val customerDao: CustomerDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : TransactionRepository {

    private suspend fun code() = activationRepo.getMerchantCode()

    override fun getAllTransactions(): Flow<List<Transaction>> =
    transactionDao.getAllTransactions().map {
        list ->
        list.map {
            entity -> entity.toDomain(
                customerName = customerDao.getCustomerById(entity.customerId)?.name ?: "",
                paymentMethodName = entity.paymentMethodId?.let {
                    paymentMethodDao.getPaymentMethodById(it)?.name
                } ?: ""
            )}
    }

    override fun getTransactionsByCustomer(customerId: Long): Flow<List<Transaction>> =
    transactionDao.getTransactionsByCustomer(customerId).map {
        list ->
        list.map {
            it.toDomain(
                paymentMethodName = it.paymentMethodId?.let {
                    paymentMethodDao.getPaymentMethodById(it)?.name
                } ?: ""
            )}
    }

    override fun getUnpaidTransactions(): Flow<List<Transaction>> =
    transactionDao.getUnpaidTransactions().map {
        list ->
        list.map {
            entity -> entity.toDomain(
                customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
            )}
    }

    override fun getTransactionsByDate(startDate: Long, endDate: Long): Flow<List<Transaction>> =
    transactionDao.getTransactionsByDate(startDate, endDate).map {
        list ->
        list.map {
            entity -> entity.toDomain(
                customerName = customerDao.getCustomerById(entity.customerId)?.name ?: ""
            )}
    }

    override suspend fun getTransactionById(id: Long): Transaction? =
    transactionDao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
        sync.pushTransaction(code(), transaction.copy(id = id))
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
        sync.pushTransaction(code(), transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(transaction))
        sync.deleteTransaction(code(), transaction.id)
    }

    override suspend fun getTotalAmountByDate(startDate: Long, endDate: Long): Double =
    transactionDao.getTotalAmountByDate(startDate, endDate)

    override suspend fun getPaidAmountByDate(startDate: Long, endDate: Long): Double =
    transactionDao.getPaidAmountByDate(startDate, endDate)

    override suspend fun getUnpaidAmountByCustomer(customerId: Long): Double =
    transactionDao.getUnpaidAmountByCustomer(customerId)
}