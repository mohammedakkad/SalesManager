package com.trader.core.sync

import com.trader.core.domain.model.SyncStatus

sealed class GlobalSyncState {
    object AllSynced : GlobalSyncState()
    data class Pending(val count: Int) : GlobalSyncState()
    data class Failed(val count: Int) : GlobalSyncState()
}

data class UnsyncedItem(
    val id: String,
    val type: UnsyncedItemType,
    val label: String,
    val syncStatus: SyncStatus
)

enum class UnsyncedItemType {
    TRANSACTION,
    CASH_BOX,
    STOCK_MOVEMENT,
    INVOICE_ITEM
}
