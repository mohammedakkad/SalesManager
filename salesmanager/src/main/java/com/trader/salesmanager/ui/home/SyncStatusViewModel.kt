package com.trader.salesmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.sync.GlobalSyncState
import com.trader.core.sync.SyncCoordinator
import com.trader.core.sync.SyncStatusObserver
import com.trader.core.sync.UnsyncedItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncStatusViewModel(
    private val observer: SyncStatusObserver,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    val syncState: StateFlow<GlobalSyncState> = observer.globalSyncState
        .stateIn(viewModelScope, SharingStarted.Eagerly, GlobalSyncState.AllSynced)

    val unsyncedItems: StateFlow<List<UnsyncedItem>> = observer.unsyncedItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun retrySync() {
        viewModelScope.launch {
            syncCoordinator.syncAll()
        }
    }
}
