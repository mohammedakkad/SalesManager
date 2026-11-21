package com.trader.core.data.repository

import androidx.room.withTransaction
import com.trader.core.data.local.dao.InventoryDao
import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.InventorySessionEntity
import com.trader.core.data.local.entity.toEntity
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.InventoryRepository
import com.trader.core.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class InventoryRepositoryImpl(
    private val inventoryDao: InventoryDao,
    private val productDao: ProductDao,
    private val stockRepo: StockRepository,
    private val merchantId: String,
    private val database: AppDatabase
) : InventoryRepository {

    override fun getActiveSession(): Flow<InventorySession?> =
        inventoryDao.getActiveSession().map { it?.toDomain() }

    override fun getSessionItems(sessionId: String): Flow<List<InventorySessionItem>> =
        inventoryDao.getSessionItems(sessionId).map { it.map { e -> e.toDomain() } }

    override fun getAllSessions(): Flow<List<InventorySession>> =
        inventoryDao.getAllSessions().map { it.map { e -> e.toDomain() } }

    override suspend fun startNewSession(merchantId: String): String = database.withTransaction {
        require(merchantId == this@InventoryRepositoryImpl.merchantId) { "Inventory merchant mismatch" }
        val sessionId = UUID.randomUUID().toString()
        inventoryDao.insertSession(
            InventorySession(
                id = sessionId, merchantId = merchantId,
                status = InventoryStatus.IN_PROGRESS, startedAt = System.currentTimeMillis()
            ).toEntity()
        )
        val items = productDao.getAllWithUnitsOnce().flatMap { relation ->
            relation.units.map { unit ->
                InventorySessionItem(
                    id = UUID.randomUUID().toString(), sessionId = sessionId,
                    productId = relation.product.id, productName = relation.product.name,
                    unitId = unit.id, unitLabel = unit.unitLabel,
                    systemQuantity = unit.quantityInStock, actualQuantity = null
                ).toEntity()
            }
        }
        inventoryDao.insertSessionItems(items)
        sessionId
    }

    override suspend fun updateSessionItem(item: InventorySessionItem) = database.withTransaction {
        require(item.actualQuantity == null || (item.actualQuantity.isFinite() && item.actualQuantity >= 0)) {
            "Inventory count must be finite and nonnegative"
        }
        val session = ownedSession(item.sessionId)
        require(session.status == InventoryStatus.IN_PROGRESS.name) { "Inventory session is closed" }
        val stored = inventoryDao.getSessionItemsOnce(item.sessionId).firstOrNull { it.id == item.id }
        requireNotNull(stored) { "Inventory item not found" }
        // Only the count is editable; preserve the captured product identity and stock snapshot.
        inventoryDao.updateSessionItem(stored.copy(actualQuantity = item.actualQuantity))
    }

    override suspend fun finishSession(sessionId: String) = database.withTransaction {
        val session = ownedSession(sessionId)
        if (session.status == InventoryStatus.FINISHED.name) return@withTransaction
        require(session.status == InventoryStatus.IN_PROGRESS.name) { "Invalid inventory session status" }
        val items = inventoryDao.getSessionItemsOnce(sessionId)
        var adjustmentCount = 0
        items.forEach { item ->
            val actual = item.actualQuantity ?: return@forEach
            require(actual.isFinite() && actual >= 0) { "Inventory count must be finite and nonnegative" }
            val diff = actual - item.systemQuantity
            if (kotlin.math.abs(diff) > 0.001) {
                stockRepo.manualAdjust(
                    productId = item.productId, unitId = item.unitId, quantity = diff,
                    type = MovementType.INVENTORY_ADJUST,
                    productName = item.productName, unitLabel = item.unitLabel,
                    note = "جرد دوري - جلسة $sessionId"
                )
                adjustmentCount++
            }
        }
        inventoryDao.updateSession(
            session.copy(
                status = InventoryStatus.FINISHED.name, finishedAt = System.currentTimeMillis(),
                totalAdjustments = adjustmentCount, syncStatus = "PENDING"
            )
        )
    }

    override suspend fun cancelSession(sessionId: String) = database.withTransaction {
        val session = ownedSession(sessionId)
        if (session.status == InventoryStatus.FINISHED.name) return@withTransaction
        inventoryDao.updateSession(
            session.copy(
                status = InventoryStatus.FINISHED.name, finishedAt = System.currentTimeMillis(),
                totalAdjustments = 0, syncStatus = "PENDING"
            )
        )
    }

    private suspend fun ownedSession(sessionId: String): InventorySessionEntity {
        val session = requireNotNull(inventoryDao.getSessionById(sessionId)) { "Inventory session not found" }
        require(session.merchantId == merchantId) { "Inventory merchant mismatch" }
        return session
    }
}
