package com.trader.core.data.repository

import com.trader.core.data.local.dao.CashBoxDao
import com.trader.core.data.local.dao.CashBoxMovementDao
import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.db.recordCashBoxBalanceChange
import com.trader.core.data.local.entity.CashBoxEntity
import com.trader.core.data.local.entity.CashBoxMovementEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.AdjustmentReason
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.CashBoxMovement
import com.trader.core.domain.model.CashBoxMovementType
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CashBoxRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class CashBoxRepositoryImpl(
    private val database: AppDatabase,
    private val dao: CashBoxDao,
    private val movementDao: CashBoxMovementDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CashBoxRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val boxMutex = Mutex()

    init {
        startRealtimeSync()
        startAutoCreation()
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { merchantCode ->
                    launch {
                        sync.observeCashBoxes(merchantCode).collect { list ->
                            list.forEach { remote ->
                                boxMutex.withLock {
                                    val local = dao.getById(remote.id)
                                    if (local == null || local.syncStatus == SyncStatus.SYNCED.name) {
                                        dao.upsert(
                                            CashBoxEntity.fromDomain(
                                                remote.copy(merchantId = merchantCode, syncStatus = SyncStatus.SYNCED)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    launch {
                        sync.observeCashBoxMovements(merchantCode).collect { list ->
                            list.forEach { remote ->
                                val local = movementDao.getById(remote.id)
                                if (local == null || local.syncStatus == SyncStatus.SYNCED.name) {
                                    movementDao.insert(
                                        CashBoxMovementEntity.fromDomain(
                                            remote.copy(merchantId = merchantCode, syncStatus = SyncStatus.SYNCED)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun startAutoCreation() {
        syncScope.launch {
            paymentMethodDao.getAllPaymentMethods().collect { methods ->
                methods.forEach { method ->
                    boxMutex.withLock {
                        val existing = dao.getByPaymentMethodId(method.id)
                        when {
                            existing == null -> createBoxFor(method.id, method.name)
                            existing.paymentMethodName != method.name -> {
                                dao.upsert(
                                    existing.copy(
                                        paymentMethodName = method.name,
                                        updatedAt = System.currentTimeMillis(),
                                        syncStatus = SyncStatus.PENDING.name
                                    )
                                )
                                pushBoxAsync(existing.id)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun createBoxFor(paymentMethodId: Long, paymentMethodName: String) {
        val merchantCode = code()
        val boxId = paymentMethodId.toString()

        val remote = if (merchantCode.isNotEmpty()) {
            runCatching { sync.fetchCashBox(merchantCode, boxId) }.getOrNull()
        } else null

        if (remote != null) {
            dao.upsert(
                CashBoxEntity.fromDomain(
                    remote.copy(merchantId = merchantCode, syncStatus = SyncStatus.SYNCED)
                )
            )
            return
        }

        val box = CashBox(
            id = boxId,
            paymentMethodId = paymentMethodId,
            paymentMethodName = paymentMethodName,
            currentBalance = 0.0,
            initialBalance = 0.0,
            initialBalanceSetAt = null,
            merchantId = merchantCode,
            syncStatus = SyncStatus.PENDING
        )
        dao.upsert(CashBoxEntity.fromDomain(box))
        pushBoxAsync(boxId)
    }

    private suspend fun ensureBoxExists(paymentMethodId: Long): CashBoxEntity? {
        val boxId = paymentMethodId.toString()
        var box = dao.getById(boxId)
        if (box == null) {
            val name = paymentMethodDao.getPaymentMethodById(paymentMethodId)?.name ?: ""
            createBoxFor(paymentMethodId, name)
            box = dao.getById(boxId)
        }
        return box
    }

    private suspend fun recordChange(
        boxId: String,
        delta: Double,
        absoluteBalance: Double?,
        type: CashBoxMovementType,
        note: String,
        relatedTransactionId: Long?,
        markAsInitial: Boolean = false
    ): String? {
        val box = dao.getById(boxId) ?: return null
        val now = System.currentTimeMillis()
        val movementId = UUID.randomUUID().toString()
        val movement = CashBoxMovementEntity(
            id = movementId,
            cashBoxId = boxId,
            paymentMethodName = box.paymentMethodName,
            type = type.name,
            amountDelta = if (absoluteBalance != null) absoluteBalance else delta,
            balanceAfter = 0.0,
            note = note,
            relatedTransactionId = relatedTransactionId,
            createdAt = now,
            merchantId = box.merchantId,
            syncStatus = SyncStatus.PENDING.name
        )

        database.recordCashBoxBalanceChange(
            boxId = boxId,
            delta = delta,
            absoluteBalance = absoluteBalance,
            updatedAt = now,
            movement = movement,
            markAsInitial = markAsInitial
        )

        pushBoxAsync(boxId)
        pushMovementAsync(movementId)
        return movementId
    }

    private fun pushBoxAsync(boxId: String) {
        syncScope.launch {
            try {
                val merchantCode = code()
                if (merchantCode.isEmpty()) return@launch
                val entity = dao.getById(boxId) ?: return@launch
                sync.pushCashBox(merchantCode, entity.toDomain())
                dao.markSynced(boxId)
            } catch (_: Exception) {}
        }
    }

    private fun pushMovementAsync(movementId: String) {
        syncScope.launch {
            try {
                val merchantCode = code()
                if (merchantCode.isEmpty()) return@launch
                val entity = movementDao.getById(movementId) ?: return@launch
                sync.pushCashBoxMovement(merchantCode, entity.toDomain())
                movementDao.markSynced(movementId)
            } catch (_: Exception) {}
        }
    }

    override fun getAllBoxes(): Flow<List<CashBox>> =
        dao.getAll().map { it.map(CashBoxEntity::toDomain) }

    override fun getAllMovements(): Flow<List<CashBoxMovement>> =
        movementDao.getAll().map { it.map(CashBoxMovementEntity::toDomain) }

    override fun getMovementsForBox(cashBoxId: String): Flow<List<CashBoxMovement>> =
        movementDao.getByCashBoxId(cashBoxId).map { it.map(CashBoxMovementEntity::toDomain) }

    override suspend fun getBoxByPaymentMethod(paymentMethodId: Long): CashBox? =
        dao.getByPaymentMethodId(paymentMethodId)?.toDomain()

    override suspend fun setInitialBalance(boxId: String, amount: Double) {
        boxMutex.withLock {
            val existing = dao.getById(boxId) ?: return
            if (existing.initialBalanceSetAt != null) return
            recordChange(
                boxId = boxId,
                delta = amount,
                absoluteBalance = amount,
                type = CashBoxMovementType.INITIAL_BALANCE,
                note = "",
                relatedTransactionId = null,
                markAsInitial = true
            )
        }
    }

    override suspend fun adjustBalance(
        boxId: String,
        newAmount: Double,
        note: String?,
        reason: AdjustmentReason
    ) {
        boxMutex.withLock {
            val existing = dao.getById(boxId) ?: return
            if (existing.initialBalanceSetAt == null) return
            val delta = newAmount - existing.currentBalance
            if (delta == 0.0) return
            recordChange(
                boxId = boxId,
                delta = delta,
                absoluteBalance = null,
                type = CashBoxMovementType.MANUAL_ADJUSTMENT,
                note = note.orEmpty(),
                relatedTransactionId = null
            )
        }
    }

    override suspend fun applyTransactionEffect(
        relatedTransactionId: Long?,
        oldPaymentMethodId: Long?, oldAmount: Double, oldWasPaid: Boolean,
        newPaymentMethodId: Long?, newAmount: Double, newWasPaid: Boolean
    ) {
        val deltas = mutableMapOf<Long, Double>()
        if (oldWasPaid && oldPaymentMethodId != null) {
            deltas.merge(oldPaymentMethodId, -oldAmount, Double::plus)
        }
        if (newWasPaid && newPaymentMethodId != null) {
            deltas.merge(newPaymentMethodId, newAmount, Double::plus)
        }

        deltas.filterValues { it != 0.0 }.forEach { (paymentMethodId, delta) ->
            val boxId = paymentMethodId.toString()
            boxMutex.withLock {
                ensureBoxExists(paymentMethodId)
                recordChange(
                    boxId = boxId,
                    delta = delta,
                    absoluteBalance = null,
                    type = CashBoxMovementType.TRANSACTION_EFFECT,
                    note = "",
                    relatedTransactionId = relatedTransactionId
                )
            }
        }
    }

    override suspend fun syncPendingBoxes() {
        val merchantCode = code()
        if (merchantCode.isEmpty()) return
        dao.getPending().forEach { entity ->
            try {
                sync.pushCashBox(merchantCode, entity.toDomain())
                dao.markSynced(entity.id)
            } catch (_: Exception) {}
        }
        movementDao.getPending().forEach { entity ->
            try {
                sync.pushCashBoxMovement(merchantCode, entity.toDomain())
                movementDao.markSynced(entity.id)
            } catch (_: Exception) {}
        }
    }
}
