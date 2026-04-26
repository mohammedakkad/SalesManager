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
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.db.upsertProductWithUnitsAndClean

class ProductRepositoryImpl(
    private val dao: ProductDao,
    private val database: AppDatabase,
    private val remote: ProductFirestoreService,
    private val activationRepo: ActivationRepository,
    private val networkMonitor: NetworkMonitor
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

                            val units = try {
                                remote.fetchUnitsForProduct(code, remoteProduct.id)
                            } catch (_: Exception) {
                                null
                            }
                            when {
                                units != null && units.isNotEmpty() -> {
                                    database.upsertProductWithUnitsAndClean(
                                        remoteProduct.toEntity(),
                                        units.map {
                                            it.toEntity()
                                        }
                                    )
                                } else -> {
                                    if (local == null) {
                                        dao.insertProduct(remoteProduct.toEntity())
                                    }
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

    override suspend fun saveProduct(product: Product, units: List<ProductUnit>): String {
        val id = product.id.ifEmpty {
            UUID.randomUUID().toString()
        }
        val now = System.currentTimeMillis()
        val mid = merchantId()

        // ✅ الإصلاح: جلب IDs الوحدات القديمة قبل الحفظ (فقط عند التعديل)
        // نحتاجها لحذف المحذوف منها في Firestore — كانت تُحذف من Room لكن تبقى في Firestore
        // فيُعيدها startRealtimeSync بعد ثوانٍ
        val deletedUnitIds: Set<String> = if (product.id.isNotEmpty()) {
            val oldIds = dao.getUnitsForProduct(id).map {
                it.id
            }.toSet()
            val newIds = units.map {
                it.id
            }.filter {
                it.isNotEmpty()
            }.toSet()
            oldIds - newIds
        } else emptySet()

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

        syncInBackground {
            remote.uploadProduct(mid, productToSave.copy(syncStatus = SyncStatus.SYNCED))
            unitsToSave.forEach {
                unit ->
                remote.uploadUnit(mid, unit.copy(syncStatus = SyncStatus.SYNCED))
            }
            // ✅ حذف الوحدات المحذوفة من Firestore حتى لا يُعيدها startRealtimeSync
            deletedUnitIds.forEach {
                unitId ->
                try {
                    remote.deleteUnit(mid, unitId, id)
                } catch (_: Exception) {}
            }
            dao.markProductSynced(id)
            unitsToSave.forEach {
                unit -> dao.markUnitSynced(unit.id)
            }
        }

        return id
    }

    override suspend fun updateProduct(product: Product) {
        val mid = merchantId()
        val updated = product.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateProduct(updated.toEntity())
        syncInBackground {
            remote.uploadProduct(mid, updated.copy(syncStatus = SyncStatus.SYNCED))
            dao.markProductSynced(updated.id)
        }
    }

    override suspend fun deleteProduct(productId: String) {
        val mid = merchantId()
        dao.deleteProduct(productId)
        syncInBackground {
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
        syncInBackground {
            remote.uploadUnit(mid, toSave.copy(syncStatus = SyncStatus.SYNCED))
            dao.markUnitSynced(toSave.id)
        }
    }

    override suspend fun updateUnit(unit: ProductUnit) {
        val mid = merchantId()
        val updated = unit.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateUnit(updated.toEntity())
        syncInBackground {
            remote.uploadUnit(mid, updated.copy(syncStatus = SyncStatus.SYNCED))
            dao.markUnitSynced(updated.id)
        }
    }

    override suspend fun deleteUnit(unitId: String) {
        val mid = merchantId()
        val productId = dao.getUnitsForProduct(unitId).firstOrNull()?.productId ?: run {
            dao.deleteUnit(unitId)
            return
        }
        dao.deleteUnit(unitId)
        syncInBackground {
            remote.deleteUnit(mid, unitId, productId)
        }
    }

    override suspend fun syncPendingProducts() {
        val mid = merchantId()
        if (mid.isEmpty()) return

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

    private fun syncInBackground(block: suspend () -> Unit) {
        syncScope.launch {
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