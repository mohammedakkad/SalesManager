package com.trader.core.data.repository

import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.dao.ProductWithUnitsRelation
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepositoryImpl(
    private val dao: ProductDao,
    private val remote: ProductFirestoreService,
    private val merchantId: String
) : ProductRepository {

    override fun getAllProducts(): Flow<List<ProductWithUnits>> =
        dao.getAllWithUnits().map { list -> list.map { it.toDomainWithUnits() } }

    override fun searchProducts(query: String): Flow<List<ProductWithUnits>> =
        dao.searchWithUnits(query).map { list -> list.map { it.toDomainWithUnits() } }

    override fun getLowStockProducts(): Flow<List<ProductWithUnits>> =
        getAllProducts().map { list -> list.filter { it.isLowStock } }

    override fun getOutOfStockProducts(): Flow<List<ProductWithUnits>> =
        getAllProducts().map { list -> list.filter { it.isOutOfStock } }

    override suspend fun getProductByBarcode(barcode: String): ProductWithUnits? =
        dao.getByBarcode(barcode)?.toDomainWithUnits()

    override suspend fun getProductById(id: String): ProductWithUnits? =
        dao.getById(id)?.toDomainWithUnits()

    override suspend fun saveProduct(product: Product, units: List<ProductUnit>): String {
        val id = product.id.ifEmpty { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val productToSave = product.copy(
            id = id, merchantId = merchantId,
            createdAt = if (product.createdAt == 0L) now else product.createdAt,
            updatedAt = now, syncStatus = SyncStatus.PENDING
        )
        dao.insertProduct(productToSave.toEntity())
        units.forEachIndexed { index, unit ->
            val unitToSave = unit.copy(
                id = unit.id.ifEmpty { UUID.randomUUID().toString() },
                productId = id,
                isDefault = if (units.any { it.isDefault }) unit.isDefault else index == 0,
                createdAt = if (unit.createdAt == 0L) now else unit.createdAt,
                updatedAt = now, syncStatus = SyncStatus.PENDING
            )
            dao.insertUnit(unitToSave.toEntity())
            trySync { remote.uploadUnit(merchantId, unitToSave) }
        }
        trySync { remote.uploadProduct(merchantId, productToSave) }
        return id
    }

    override suspend fun updateProduct(product: Product) {
        val updated = product.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateProduct(updated.toEntity())
        trySync { remote.uploadProduct(merchantId, updated) }
    }

    override suspend fun deleteProduct(productId: String) {
        dao.deleteProduct(productId)
        trySync { remote.deleteProduct(merchantId, productId) }
    }

    override suspend fun saveUnit(unit: ProductUnit) {
        val toSave = unit.copy(
            id = unit.id.ifEmpty { UUID.randomUUID().toString() },
            updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING
        )
        dao.insertUnit(toSave.toEntity())
        trySync { remote.uploadUnit(merchantId, toSave) }
    }

    override suspend fun updateUnit(unit: ProductUnit) {
        val updated = unit.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
        dao.updateUnit(updated.toEntity())
        trySync { remote.uploadUnit(merchantId, updated) }
    }

    override suspend fun deleteUnit(unitId: String) {
        dao.deleteUnit(unitId)
        trySync { remote.deleteUnit(merchantId, unitId) }
    }

    override suspend fun syncPendingProducts() {
        dao.getPendingProducts().forEach { entity ->
            try { remote.uploadProduct(merchantId, entity.toDomain()); dao.markProductSynced(entity.id) } catch (_: Exception) {}
        }
        dao.getPendingUnits().forEach { entity ->
            try { remote.uploadUnit(merchantId, entity.toDomain()); dao.markUnitSynced(entity.id) } catch (_: Exception) {}
        }
    }

    private suspend fun trySync(block: suspend () -> Unit) {
        try { block() } catch (_: Exception) {}
    }

    private fun ProductWithUnitsRelation.toDomainWithUnits() = ProductWithUnits(
        product = product.toDomain(),
        units = units.map { it.toDomain() }
    )
}
