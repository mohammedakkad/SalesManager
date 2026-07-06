package com.trader.core.data.repository

import com.trader.core.data.local.dao.CashBoxDao
import com.trader.core.data.local.dao.PaymentMethodDao
import com.trader.core.data.local.entity.CashBoxEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.CashBox
import com.trader.core.domain.model.SyncStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.CashBoxRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CashBoxRepositoryImpl(
    private val dao: CashBoxDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sync: FirebaseSyncService,
    private val activationRepo: ActivationRepository
) : CashBoxRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // يمنع سباق إنشاء/تعديل متزامن على نفس الصندوق
    private val boxMutex = Mutex()

    init {
        startRealtimeSync()
        startAutoCreation()
    }

    private suspend fun code() = activationRepo.getMerchantCode()

    // ── مزامنة فورية من Firebase (نفس نمط CustomerRepositoryImpl) ──
    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collectLatest { code ->
                    sync.observeCashBoxes(code).collect { list ->
                        list.forEach { remote ->
                            boxMutex.withLock {
                                val local = dao.getById(remote.id)
                                // لا نلمس الصناديق PENDING — تغييراتها المحلية لم تُرفع بعد
                                if (local == null || local.syncStatus == SyncStatus.SYNCED.name) {
                                    dao.upsert(
                                        CashBoxEntity.fromDomain(
                                            remote.copy(merchantId = code, syncStatus = SyncStatus.SYNCED)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    // ── إنشاء تلقائي: صندوق لكل طريقة دفع لا تملك صندوقاً بعد ──
    // (حذف التتالي عند حذف طريقة الدفع يتم في PaymentMethodRepositoryImpl)
    private fun startAutoCreation() {
        syncScope.launch {
            paymentMethodDao.getAllPaymentMethods().collect { methods ->
                methods.forEach { method ->
                    boxMutex.withLock {
                        val existing = dao.getByPaymentMethodId(method.id)
                        when {
                            existing == null -> createBoxFor(method.id, method.name)
                            existing.paymentMethodName != method.name -> {
                                // تحديث الاسم المنسوخ عند إعادة تسمية طريقة الدفع
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

    /**
     * ينشئ صندوقاً جديداً لطريقة دفع. قبل الإنشاء يتحقق من Firebase:
     * إذا كان الصندوق موجوداً عن بُعد (جهاز آخر أنشأه وحدّد رصيده)
     * نعتمد النسخة البعيدة بدل الكتابة فوقها بصفر.
     * يجب استدعاؤها داخل boxMutex.
     */
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

    /** رفع الحالة الحالية للصندوق إلى Firebase ثم وسمه SYNCED */
    private fun pushBoxAsync(boxId: String) {
        syncScope.launch {
            try {
                val merchantCode = code()
                if (merchantCode.isEmpty()) return@launch
                val entity = dao.getById(boxId) ?: return@launch
                sync.pushCashBox(merchantCode, entity.toDomain())
                dao.markSynced(boxId)
            } catch (_: Exception) {
                // يبقى PENDING — سيُعاد رفعه عبر syncPendingBoxes()
            }
        }
    }

    override fun getAllBoxes(): Flow<List<CashBox>> =
        dao.getAll().map { it.map(CashBoxEntity::toDomain) }

    override suspend fun getBoxByPaymentMethod(paymentMethodId: Long): CashBox? =
        dao.getByPaymentMethodId(paymentMethodId)?.toDomain()

    override suspend fun setInitialBalance(boxId: String, amount: Double) {
        boxMutex.withLock {
            val existing = dao.getById(boxId) ?: return
            // يُحدَّد مرة واحدة فقط
            if (existing.initialBalanceSetAt != null) return
            val now = System.currentTimeMillis()
            dao.upsert(
                existing.copy(
                    initialBalance = amount,
                    currentBalance = amount,
                    initialBalanceSetAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING.name
                )
            )
        }
        pushBoxAsync(boxId)
    }

    override suspend fun applyTransactionEffect(
        oldPaymentMethodId: Long?, oldAmount: Double, oldWasPaid: Boolean,
        newPaymentMethodId: Long?, newAmount: Double, newWasPaid: Boolean
    ) {
        val now = System.currentTimeMillis()

        // صافي الأثر لكل صندوق — يغطي الحالات الأربع:
        //   غير مدفوعة → مدفوعة: +جديد
        //   مدفوعة → غير مدفوعة: -قديم
        //   تغيّرت الطريقة: -قديم من الصندوق القديم، +جديد للصندوق الجديد
        //   تغيّر المبلغ فقط: صافي الفرق على نفس الصندوق
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
                if (dao.getById(boxId) == null) {
                    val name = paymentMethodDao.getPaymentMethodById(paymentMethodId)?.name ?: ""
                    createBoxFor(paymentMethodId, name)
                }
                // تعديل ذرّي على Room — offline-first، ويوسم PENDING
                dao.applyDelta(boxId, delta, now)
            }
            pushBoxAsync(boxId)
        }
    }

    override suspend fun syncPendingBoxes() {
        val merchantCode = code()
        if (merchantCode.isEmpty()) return
        dao.getPending().forEach { entity ->
            try {
                sync.pushCashBox(merchantCode, entity.toDomain())
                dao.markSynced(entity.id)
            } catch (_: Exception) {
                // يبقى PENDING للمحاولة القادمة
            }
        }
    }
}
