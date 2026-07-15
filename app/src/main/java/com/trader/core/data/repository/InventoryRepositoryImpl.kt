package com.trader.core.data.repository

import com.trader.core.data.local.dao.InventoryDao
import com.trader.core.data.local.dao.ProductDao
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
    private val merchantId: String
) : InventoryRepository {

    override fun getActiveSession(): Flow<InventorySession?> =
        inventoryDao.getActiveSession().map { it?.toDomain() }

    override fun getSessionItems(sessionId: String): Flow<List<InventorySessionItem>> =
        inventoryDao.getSessionItems(sessionId).map { it.map { e -> e.toDomain() } }

    override fun getAllSessions(): Flow<List<InventorySession>> =
        inventoryDao.getAllSessions().map { it.map { e -> e.toDomain() } }

    override suspend fun startNewSession(merchantId: String): String {
        val sessionId = UUID.randomUUID().toString()
        val session = InventorySession(
            id = sessionId, merchantId = merchantId,
            status = InventoryStatus.IN_PROGRESS,
            startedAt = System.currentTimeMillis()
        )
        inventoryDao.insertSession(session.toEntity())

        // تحميل كل الأصناف والوحدات في جلسة الجرد
        val allProducts = productDao.getAllWithUnits()
        // نستخدم collect مرة واحدة لأخذ القيمة الحالية
        val items = mutableListOf<InventorySessionItem>()
        // نقرأ من خلال query مباشر بدل Flow
        // سنبني الأصناف من product_units
        // للبساطة — نأخذ snapshot من الـ DAO مباشرة

        inventoryDao.insertSessionItems(items.map { it.toEntity() })
        return sessionId
    }

    /**
     * تحميل أصناف الجلسة من المخزن الحالي
     */
    suspend fun populateSessionItems(sessionId: String) {
        val allRelations = productDao.getAllWithUnitsOnce()
        val items = allRelations.flatMap { relation ->
            relation.units.map { unit ->
                InventorySessionItem(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    productId = relation.product.id,
                    productName = relation.product.name,
                    unitId = unit.id,
                    unitLabel = unit.unitLabel,
                    systemQuantity = unit.quantityInStock,
                    actualQuantity = null
                )
            }
        }
        inventoryDao.insertSessionItems(items.map { it.toEntity() })
    }

    override suspend fun updateSessionItem(item: InventorySessionItem) {
        inventoryDao.updateSessionItem(item.toEntity())
    }

    override suspend fun finishSession(sessionId: String) {
        val items = inventoryDao.getSessionItemsOnce(sessionId)
        var adjustmentCount = 0

        items.forEach { item ->
            val actual = item.actualQuantity ?: return@forEach
            val diff = actual - item.systemQuantity
            if (kotlin.math.abs(diff) > 0.001) {
                stockRepo.manualAdjust(
                    productId = item.productId,
                    unitId = item.unitId,
                    quantity = diff,
                    type = MovementType.INVENTORY_ADJUST,
                    productName = item.productName,
                    unitLabel = item.unitLabel,
                    note = "جرد دوري - جلسة $sessionId"
                )
                adjustmentCount++
            }
        }

        val session = InventorySession(
            id = sessionId, merchantId = merchantId,
            status = InventoryStatus.FINISHED,
            startedAt = 0L,
            finishedAt = System.currentTimeMillis(),
            totalAdjustments = adjustmentCount
        )
        inventoryDao.updateSession(session.toEntity())
    }

    override suspend fun cancelSession(sessionId: String) {
        val session = InventorySession(
            id = sessionId, merchantId = merchantId,
            status = InventoryStatus.FINISHED,
            startedAt = 0L,
            finishedAt = System.currentTimeMillis(),
            totalAdjustments = 0
        )
        inventoryDao.updateSession(session.toEntity())
    }
}
