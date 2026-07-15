package com.trader.core.data.repository

import androidx.room.withTransaction
import com.trader.core.data.local.dao.LowStockAlertDao
import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.LowStockAlertStateEntity
import com.trader.core.domain.model.StockLevel
import com.trader.core.domain.model.stockLevel
import com.trader.core.domain.repository.LowStockAlertCandidate
import com.trader.core.domain.repository.LowStockAlertRepository

class LowStockAlertRepositoryImpl(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val alertDao: LowStockAlertDao
) : LowStockAlertRepository {

    override suspend fun getNotificationCandidates(
        nowMillis: Long,
        cooldownMillis: Long
    ): List<LowStockAlertCandidate> = database.withTransaction {
        val previousByUnit = alertDao.getAll().associateBy { it.unitId }
        val currentUnits = productDao.getAllWithUnitsOnce().flatMap { relation ->
            relation.units.map { unit ->
                Triple(relation.product.id, unit, unit.toDomain().stockLevel)
            }
        }

        val candidates = currentUnits.mapNotNull { (productId, unit, status) ->
            if (status == StockLevel.AVAILABLE) return@mapNotNull null
            val previous = previousByUnit[unit.id]
            val lastNotifiedStatus = previous?.lastNotifiedStatus?.let(StockLevel::valueOf)
            val cooldownExpired = previous?.lastNotifiedAt?.let {
                nowMillis - it >= cooldownMillis
            } ?: true
            val shouldNotify = lastNotifiedStatus == null ||
                lastNotifiedStatus == StockLevel.LOW && status == StockLevel.OUT ||
                cooldownExpired

            if (shouldNotify) {
                LowStockAlertCandidate(
                    unitId = unit.id,
                    productId = productId,
                    status = status
                )
            } else {
                null
            }
        }

        val currentStates = currentUnits.map { (_, unit, status) ->
            val previous = previousByUnit[unit.id]
            LowStockAlertStateEntity(
                unitId = unit.id,
                lastStatus = status.name,
                lastNotifiedStatus = if (status == StockLevel.AVAILABLE) {
                    null
                } else {
                    previous?.lastNotifiedStatus
                },
                lastNotifiedAt = if (status == StockLevel.AVAILABLE) {
                    null
                } else {
                    previous?.lastNotifiedAt
                }
            )
        }
        if (currentStates.isNotEmpty()) alertDao.upsertAll(currentStates)
        alertDao.deleteOrphans()
        candidates
    }

    override suspend fun markNotified(
        candidates: List<LowStockAlertCandidate>,
        notifiedAt: Long
    ) {
        database.withTransaction {
            candidates.forEach {
                alertDao.markNotified(it.unitId, it.status.name, notifiedAt)
            }
        }
    }
}
