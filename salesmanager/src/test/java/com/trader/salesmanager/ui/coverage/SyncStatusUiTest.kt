package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.Column
import com.trader.core.domain.model.SyncStatus
import com.trader.core.sync.*
import com.trader.salesmanager.ui.home.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncStatusUiTest:OfflineSellerUiTest() {
    @Test fun statusIconsDistinguishSyncedPendingAndFailedAndOpenDetails() {
        val state=MutableStateFlow<GlobalSyncState>(GlobalSyncState.AllSynced);var opened=0
        show { SyncStatusIconButton(state.collectAsState().value,{opened++}) }
        ui.onNodeWithContentDescription("البيانات محفوظة في السحابة").performClick();assertEquals(1,opened)
        ui.runOnIdle { state.value=GlobalSyncState.Pending(2) }
        ui.onNodeWithContentDescription("جاري المزامنة").performClick();assertEquals(2,opened)
        ui.runOnIdle { state.value=GlobalSyncState.Failed(2) }
        ui.onNodeWithContentDescription("فشل في المزامنة — اضغط للتفاصيل").performClick();assertEquals(3,opened)
    }
    @Test fun failedSheetIdentifiesRecordsAndRetryIsExplicit() {
        var retries=0
        val items=listOf(UnsyncedItem("tx",UnsyncedItemType.TRANSACTION,"فاتورة #41",SyncStatus.FAILED),
            UnsyncedItem("box",UnsyncedItemType.CASH_BOX,"صندوق نقد",SyncStatus.PENDING),
            UnsyncedItem("stock",UnsyncedItemType.STOCK_MOVEMENT,"حركة أرز",SyncStatus.PENDING))
        show { SyncStatusBottomSheet(GlobalSyncState.Failed(3),items,{}, {retries++}) }
        ui.onNodeWithText("خطأ في المزامنة").assertExists()
        ui.onNodeWithText("فاتورة #41").assertExists();assertEquals(0,retries)
        ui.onNodeWithText("إعادة المزامنة الآن").performClick();assertEquals(1,retries)
    }
    @Test fun syncedSheetShowsNoPendingRecords() {
        show { SyncStatusBottomSheet(GlobalSyncState.AllSynced,emptyList(),{}, {}) }
        ui.onNodeWithText("كل البيانات محفوظة").assertExists()
        ui.onNodeWithText("لا يوجد شيء معلق").assertExists()
    }
}
