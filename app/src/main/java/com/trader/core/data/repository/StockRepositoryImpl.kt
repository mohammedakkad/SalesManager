package com.trader.core.data.repository

import com.trader.core.data.local.dao.ProductDao
import com.trader.core.data.local.dao.StockMovementDao
import com.trader.core.data.local.entity.toEntity
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class StockRepositoryImpl(
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao,
    private val remote: ProductFirestoreService,
    private val merchantId: String
) : StockRepository {

    override fun getMovementsForProduct(productId: String, unitId: String): Flow<List<StockMovement>> =
    movementDao.getForProductUnit(productId, unitId).map {
        it.map {
            e -> e.toDomain()
        }
    }

    override fun getMovementsForTransaction(transactionId: Long): Flow<List<StockMovement>> =
    movementDao.getForTransaction(transactionId).map {
        it.map {
            e -> e.toDomain()
        }
    }

    override suspend fun deductStock(
        productId: String, unitId: String, quantity: Double,
        transactionId: Long, productName: String, unitLabel: String
    ) = applyMovement(productId, unitId, -quantity, MovementType.SALE_OUT, transactionId, productName, unitLabel)

    override suspend fun returnStock(
        productId: String, unitId: String, quantity: Double,
        transactionId: Long, productName: String, unitLabel: String
    ) = applyMovement(productId, unitId, +quantity, MovementType.RETURN_IN, transactionId, productName, unitLabel)

    override suspend fun manualAdjust(
        productId: String, unitId: String, quantity: Double,
        type: MovementType, productName: String, unitLabel: String, note: String
    ) = applyMovement(productId, unitId, quantity, type, null, productName, unitLabel, note)

    override suspend fun syncPendingMovements() {
        movementDao.getPending().forEach {
            entity ->
            try {
                remote.uploadMovement(merchantId, entity.toDomain()); movementDao.markSynced(entity.id)
            } catch (_: Exception) {}
        }
        productDao.getPendingUnits().forEach {
            unit ->
            try {
                remote.updateRemoteQuantity(merchantId, unit.id, unit.quantityInStock); productDao.markUnitSynced(unit.id)
            } catch (_: Exception) {}
        }
    }

    suspend fun detectConflicts(): List<StockConflict> {
        val conflicts = mutableListOf<StockConflict>()
        productDao.getPendingUnits().forEach {
            unitEntity ->
            try {
                val remoteQty = remote.getRemoteQuantity(merchantId, unitEntity.id) ?: return@forEach
                val localQty = unitEntity.quantityInStock
                if (kotlin.math.abs(remoteQty - localQty) > 0.001) {
                    conflicts.add(StockConflict(unitEntity.id, "", unitEntity.unitLabel, localQty, remoteQty))
                }
            } catch (_: Exception) {}
        }
        return conflicts
    }

    suspend fun resolveConflictWithServer(unitId: String) {
        try {
            remote.getRemoteQuantity(merchantId, unitId)?.let {
                productDao.updateQuantity(unitId, it)
            }
        } catch (_: Exception) {}
    }

    suspend fun resolveConflictWithLocal(unitId: String) {
        try {
            productDao.getQuantity(unitId)?.let {
                remote.updateRemoteQuantity(merchantId, unitId, it)
            }
        } catch (_: Exception) {}
    }

    private suspend fun applyMovement(
        productId: String, unitId: String, delta: Double,
        type: MovementType, relatedTransactionId: Long? = null,
        productName: String, unitLabel: String, note: String = ""
    ) {
        val currentQty = productDao.getQuantity(unitId) ?: 0.0
        val newQty = (currentQty + delta).coerceAtLeast(0.0)

        // ✅ Local first — فوري بدون انتظار
        productDao.updateQuantity(unitId, newQty)

        val movement = StockMovement(
            id = UUID.randomUUID().toString(),
            productId = productId, productName = productName,
            unitId = unitId, unitLabel = unitLabel,
            movementType = type, quantity = delta,
            quantityBefore = currentQty, quantityAfter = newQty,
            relatedTransactionId = relatedTransactionId,
            note = note, merchantId = merchantId,
            syncStatus = SyncStatus.PENDING
        )
        movementDao.insert(movement.toEntity())

        // ✅ Sync في الخلفية — لا يوقف العملية أبداً
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                remote.uploadMovement(merchantId, movement)
                remote.updateRemoteQuantity(merchantId, unitId, newQty)
                movementDao.markSynced(movement.id)
                productDao.markUnitSynced(unitId)
            } catch (_: Exception) {
                // يبقى PENDING — سيُرفع عند syncPendingMovements
            }
        }
    }
}

data class StockConflict(
    val unitId: String,
    val productName: String,
    val unitLabel: String,
    val localQuantity: Double,
    val remoteQuantity: Double
)