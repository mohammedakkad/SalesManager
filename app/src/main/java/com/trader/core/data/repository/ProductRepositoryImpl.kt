package com.trader.core.data.repository

import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.dao.ProductWithUnitsRelation
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import com.trader.core.util.NetworkMonitor
import androidx.room.withTransaction
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.db.upsertProductWithUnitsAndClean

class ProductRepositoryImpl(
    private val dao: ProductDao,
    private val database: AppDatabase,
    private val remote: ProductFirestoreService,
    private val activationRepo: ActivationRepository,
    private val networkMonitor: NetworkMonitor,
    private val useOutboxFence: Boolean = false
) : ProductRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
        startPendingSyncOnReconnect()
    }

    private fun startPendingSyncOnReconnect() {
        syncScope.launch {
            networkMonitor.isOnlineFlow
            .filter {
                it
            }
            .collect {
                syncPendingProducts()
            }
        }
    }

    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
            .filter {
                it.isNotEmpty()
            }
            .distinctUntilChanged()
            .collectLatest {
                code ->
                remote.observeProducts(code).collect {
                    remoteProducts ->
                    remoteProducts.forEach {
                        remoteProduct ->
                        launch {
                            val local = dao.getById(remoteProduct.id)
                            // ✅ updatedAt كافٍ وحده:
                            // - إذا تطابق → نفس الإصدار (سواء PENDING أو SYNCED) → تجاهل
                            // - شرط syncStatus كان يُبطل الـ guard: الصنف يُرفع محلياً
                            //   بـ PENDING، فيُطلق Firestore قبل markSynced → guard يفشل
                            //   → fetchUnitsForProduct لكل صنف رغم عدم تغيره
                            val isUnchanged = local != null &&
                            local.product.updatedAt == remoteProduct.updatedAt
                            if (isUnchanged) return@launch

                            val sequenceBefore = if (useOutboxFence) database.saleOutboxDao().latestSequence() else 0
                            if (useOutboxFence && database.saleOutboxDao().hasOutstandingFor(code)) return@launch
                            val units = try {
                                remote.fetchUnitsForProduct(code, remoteProduct.id)
                            } catch (_: Exception) {
                                null
                            }
                            database.withTransaction {
                                if (useOutboxFence && (database.saleOutboxDao().hasOutstandingFor(code) || database.saleOutboxDao().latestSequence() != sequenceBefore)) return@withTransaction
                                when {
                                    units != null && units.isNotEmpty() -> database.upsertProductWithUnitsAndClean(remoteProduct.toEntity(), units.map { it.toEntity() })
                                    local == null -> dao.insertProduct(remoteProduct.toEntity())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun merchantId(): String = activationRepo.getMerchantCode()

    override fun getAllProducts(): Flow<List<ProductWithUnits>> =
    dao.getAllWithUnits()
    .map {
        entities -> entities.map {
            it.toDomainWithUnits()
        }
    }
    .distinctUntilChanged()

    override fun searchProducts(query: String): Flow<List<ProductWithUnits>> =
    dao.searchWithUnits(query).map {
        list -> list.map {
            it.toDomainWithUnits()
        }
    }

    override fun getLowStockProducts(): Flow<List<ProductWithUnits>> =
    getAllProducts().map {
        list -> list.filter {
            it.isLowStock
        }
    }

    override fun getOutOfStockProducts(): Flow<List<ProductWithUnits>> =
    getAllProducts().map {
        list -> list.filter {
            it.isOutOfStock
        }
    }

    override suspend fun getProductByBarcode(barcode: String): ProductWithUnits? =
    dao.getByBarcode(barcode)?.toDomainWithUnits()

    override suspend fun getProductById(id: String): ProductWithUnits? =
    dao.getById(id)?.toDomainWithUnits()

    override suspend fun getBarcodeConflict(barcode: String, excludeProductId: String?): String? {
        if (barcode.isBlank()) return null
        val existing = dao.getByBarcode(barcode) ?: return null
        if (existing.product.id == excludeProductId) return null
        return existing.product.name
    }

    override suspend fun saveProduct(
        product: Product,
        units: List<ProductUnit>,
        deletedUnitIds: Set<String>
    ): String {
        val id = product.id.ifEmpty {
            UUID.randomUUID().toString()
        }
        val now = System.currentTimeMillis()
        val mid = merchantId()

        val productToSave = product.copy(
            id = id,
            merchantId = mid,
            createdAt = if (product.createdAt == 0L) now else product.createdAt,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING
        )
        val unitsToSave = units.mapIndexed {
            index, unit ->
            unit.copy(
                id = unit.id.ifEmpty {
                    UUID.randomUUID().toString()
                },
                productId = id,
                isDefault = if (units.any {
                    it.isDefault
                }) unit.isDefault else index == 0,
                createdAt = if (unit.createdAt == 0L) now else unit.createdAt,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }

        database.upsertProductWithUnitsAndClean(
            productToSave.toEntity(),
            unitsToSave.map {
                it.toEntity()
            }
        )

        syncInBackground(mid) {
            // ✅ markSynced فوراً بعد كل upload منفرد
            // بدلاً من الانتظار حتى نهاية كل العمليات
            // هذا يجعل badge "جاري مزامنة" يختفي بمجرد نجاح كل عملية
            remote.uploadProduct(mid, productToSave.copy(syncStatus = SyncStatus.SYNCED))
            dao.markProductSynced(id) // ← badge يختفي هنا مباشرة

            unitsToSave.forEach {
                unit ->
                remote.uploadUnit(mid, unit.copy(syncStatus = SyncStatus.SYNCED))
                dao.markUnitSynced(unit.id) // ← بعد كل وحدة
            }

            deletedUnitIds.forEach {
                unitId ->
                try {
                    remote.deleteUnit(mid, unitId, id)
                } catch (_: Exception) {}
            }
        }

        return id
    }

    override suspend fun updateProduct(product: Product) {
        val mid = merchantId()
        val updated = product.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateProduct(updated.toEntity())
        syncInBackground(mid) {
            remote.uploadProduct(mid, updated.copy(syncStatus = SyncStatus.SYNCED))
            dao.markProductSynced(updated.id)
        }
    }

    override suspend fun deleteProduct(productId: String) {
        val mid = merchantId()
        dao.deleteProduct(productId)
        syncInBackground(mid) {
            remote.deleteProduct(mid, productId)
        }
    }

    override suspend fun saveUnit(unit: ProductUnit) {
        val mid = merchantId()
        val toSave = unit.copy(
            id = unit.id.ifEmpty {
                UUID.randomUUID().toString()
            },
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        dao.insertUnit(toSave.toEntity())
        syncInBackground(mid) {
            remote.uploadUnit(mid, toSave.copy(syncStatus = SyncStatus.SYNCED))
            dao.markUnitSynced(toSave.id)
        }
    }

    override suspend fun updateUnit(unit: ProductUnit) {
        val mid = merchantId()
        val updated = unit.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateUnit(updated.toEntity())
        syncInBackground(mid) {
            remote.uploadUnit(mid, updated.copy(syncStatus = SyncStatus.SYNCED))
            dao.markUnitSynced(updated.id)
        }
    }

    override suspend fun deleteUnit(unitId: String) {
        val mid = merchantId()
        val productId = dao.getProductIdForUnit(unitId) ?: run {
            dao.deleteUnit(unitId)
            return
        }
        dao.deleteUnit(unitId)
        syncInBackground(mid) {
            remote.deleteUnit(mid, unitId, productId)
        }
    }

    override suspend fun syncPendingProducts() {
        val mid = merchantId()
        if (mid.isEmpty()) return
        if (useOutboxFence && database.saleOutboxDao().hasOutstandingFor(mid)) return

        dao.getPendingProducts().forEach {
            entity ->
            try {
                remote.uploadProduct(mid, entity.toDomain().copy(syncStatus = SyncStatus.SYNCED))
                dao.markProductSynced(entity.id)
            } catch (_: Exception) {}
        }
        dao.getPendingUnits().forEach {
            entity ->
            try {
                remote.uploadUnit(mid, entity.toDomain().copy(syncStatus = SyncStatus.SYNCED))
                dao.markUnitSynced(entity.id)
            } catch (_: Exception) {}
        }
    }

    private fun syncInBackground(merchant: String, block: suspend () -> Unit) {
        syncScope.launch {
            if (useOutboxFence && (merchantId() != merchant || database.saleOutboxDao().hasOutstandingFor(merchant))) return@launch
            try {
                block()
            } catch (_: Exception) {}
        }
    }

    private fun ProductWithUnitsRelation.toDomainWithUnits() = ProductWithUnits(
        product = product.toDomain(),
        units = units.map {
            it.toDomain()
        }
    )
}