package com.trader.core.sync

import com.trader.core.data.local.dao.CashBoxDao
import com.trader.core.data.local.dao.CashBoxMovementDao
import com.trader.core.data.local.dao.InvoiceItemDao
import com.trader.core.data.local.dao.StockMovementDao
import com.trader.core.data.local.dao.TransactionDao
import com.trader.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncStatusObserver(
    private val transactionDao: TransactionDao,
    private val cashBoxDao: CashBoxDao,
    private val cashBoxMovementDao: CashBoxMovementDao,
    private val stockMovementDao: StockMovementDao,
    private val invoiceItemDao: InvoiceItemDao
) {

    private val allUnsyncedItems: Flow<List<UnsyncedItem>> = combine(
        transactionDao.observeUnsynced(),
        cashBoxDao.observeUnsynced(),
        cashBoxMovementDao.observeUnsynced(),
        stockMovementDao.observeUnsynced()
    ) { transactions, cashBoxes, cashBoxMovements, stockMovements ->
        val items = mutableListOf<UnsyncedItem>()

        transactions.forEach { entity ->
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.PENDING)
            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(Date(entity.date))
            items.add(
                UnsyncedItem(
                    id = entity.id.toString(),
                    type = UnsyncedItemType.TRANSACTION,
                    label = "${String.format("%.0f", entity.amount)} — $dateStr",
                    syncStatus = status
                )
            )
        }

        cashBoxes.forEach { entity ->
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.PENDING)
            items.add(
                UnsyncedItem(
                    id = entity.id,
                    type = UnsyncedItemType.CASH_BOX,
                    label = entity.paymentMethodName,
                    syncStatus = status
                )
            )
        }

        cashBoxMovements.forEach { entity ->
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.PENDING)
            items.add(
                UnsyncedItem(
                    id = entity.id,
                    type = UnsyncedItemType.CASH_BOX,
                    label = entity.paymentMethodName,
                    syncStatus = status
                )
            )
        }

        stockMovements.forEach { entity ->
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.PENDING)
            val qty = if (entity.quantity == entity.quantity.toLong().toDouble())
                entity.quantity.toLong().toString()
            else String.format("%.1f", entity.quantity)
            items.add(
                UnsyncedItem(
                    id = entity.id,
                    type = UnsyncedItemType.STOCK_MOVEMENT,
                    label = "${entity.productName} ($qty ${entity.unitLabel})",
                    syncStatus = status
                )
            )
        }

        items.sortedWith(compareByDescending { it.syncStatus == SyncStatus.FAILED })
    }

    val globalSyncState: Flow<GlobalSyncState> = combine(
        allUnsyncedItems,
        invoiceItemDao.observeUnsyncedCount()
    ) { items, invoiceItemUnsyncedCount ->
        val totalCount = items.size + invoiceItemUnsyncedCount
        when {
            totalCount == 0 -> GlobalSyncState.AllSynced
            items.any { it.syncStatus == SyncStatus.FAILED } ->
                GlobalSyncState.Failed(totalCount)
            else -> GlobalSyncState.Pending(totalCount)
        }
    }

    val unsyncedItems: Flow<List<UnsyncedItem>> = allUnsyncedItems
}
