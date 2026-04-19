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
    private val activationRepo: ActivationRepository
) : ProductRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
    syncScope.launch {
        activationRepo.observeMerchantCode()
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .collectLatest { code ->
                remote.observeProducts(code).collect { products ->
                    products.forEach { product ->
                        launch {
                            val units = remote.fetchUnitsForProduct(code, product.id)
                            if (units.isNotEmpty()) {
                                dao.upsertProductWithUnits(
                                    product.toEntity(),
                                    units.map { it.toEntity() }
                                )
                                dao.deleteRemovedUnits(product.id, units.map { it.id })
                            } else {
                                // لا إنترنت أو subcollection فارغة — حدّث المنتج فقط بدون المساس بالوحدات
                                dao.insertProduct(product.toEntity())
                            }
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

    // ── حفظ وتعديل — LOCAL FIRST ──────────────────────────────────

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

        // عملية ذرية — المنتج والوحدات معاً
        dao.upsertProductWithUnits(
            productToSave.toEntity(),
            unitsToSave.map {
                it.toEntity()
            }
        )
        dao.deleteRemovedUnits(id, unitsToSave.map {
            it.id
        })

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
        val updated = product.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
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
        val updated = unit.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        dao.updateUnit(updated.toEntity())
        syncInBackground {
            remote.uploadUnit(mid, updated.copy(syncStatus = SyncStatus.SYNCED))
            dao.markUnitSynced(updated.id)
        }
    }

    override suspend fun deleteUnit(unitId: String) {
        val mid = merchantId()
        // نجلب productId من Room لأن deleteUnit في Firestore يحتاجه
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