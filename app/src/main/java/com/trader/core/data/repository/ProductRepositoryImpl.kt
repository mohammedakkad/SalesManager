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

class ProductRepositoryImpl(
    private val dao: ProductDao,
    private val remote: ProductFirestoreService,
    private val activationRepo: ActivationRepository      // ✅ بدلاً من String ثابت
) : ProductRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    /**
     * ✅ يراقب merchantCode عبر Flow — بمجرد توفّره يبدأ الاستماع لـ Firestore.
     * هذا يحل مشكلة merchantId الفارغ عند أول تثبيت.
     * collectLatest يُلغي الـ listener القديم تلقائياً إذا تغيّر الكود.
     */
    private fun startRealtimeSync() {
        syncScope.launch {
            activationRepo.observeMerchantCode()
            .filter {
                it.isNotEmpty()
            }
            .distinctUntilChanged()
            .collectLatest {
                code ->
                // مراقبة المنتجات
                launch {
                    remote.observeProducts(code).collect {
                        products ->
                        products.forEach {
                            product ->
                            dao.insertProduct(product.toEntity())
                        }
                    }
                }
                // مراقبة الوحدات — try/catch لتجنب FOREIGN KEY crash
                // إذا وصلت الوحدة قبل المنتج الأب، تُتجاهل وتُزامَن لاحقاً
                launch {
                    remote.observeUnits(code).collect {
                        units ->
                        units.forEach {
                            unit ->
                            try {
                                dao.insertUnit(unit.toEntity())
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    private suspend fun merchantId(): String = activationRepo.getMerchantCode()

    // ── استعلامات ────────────────────────────────────────────────

    override fun getAllProducts(): Flow<List<ProductWithUnits>> =
    dao.getAllWithUnits().map {
        list -> list.map {
            it.toDomainWithUnits()
        }
    }

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

    // ── حفظ وتعديل — LOCAL FIRST ─────────────────────────────────

    override suspend fun saveProduct(product: Product, units: List<ProductUnit>): String {
        val id = product.id.ifEmpty {
            UUID.randomUUID().toString()
        }
        val now = System.currentTimeMillis()
        val mid = merchantId()

        val productToSave = product.copy(
            id = id, merchantId = mid,
            createdAt = if (product.createdAt == 0L) now else product.createdAt,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING
        )

        dao.insertProduct(productToSave.toEntity())

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
        unitsToSave.forEach {
            dao.insertUnit(it.toEntity())
        }

        // رفع فوري لـ Firestore
        syncInBackground {
            remote.uploadProduct(mid, productToSave.copy(syncStatus = SyncStatus.SYNCED))
            unitsToSave.forEach {
                remote.uploadUnit(mid, it.copy(syncStatus = SyncStatus.SYNCED))
            }
            dao.markProductSynced(id)
            unitsToSave.forEach {
                dao.markUnitSynced(it.id)
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
        dao.deleteUnit(unitId)
        syncInBackground {
            remote.deleteUnit(mid, unitId)
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
            } catch (_: Exception) {
                /* يُعاد لاحقاً عبر syncPendingProducts */
            }
        }
    }

    private fun ProductWithUnitsRelation.toDomainWithUnits() = ProductWithUnits(
        product = product.toDomain(),
        units = units.map {
            it.toDomain()
        }
    )
}