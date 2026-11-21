package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.sync.*
import com.trader.salesmanager.ui.home.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeDashboardUiTest:OfflineSellerUiTest() {
    private val vm=mockk<HomeViewModel>(relaxed=true)
    private val sync=mockk<SyncStatusViewModel>(relaxed=true)
    private val state=MutableStateFlow(HomeUiState())
    @Before fun fixture() {
        every { vm.uiState } returns state
        every { sync.syncState } returns MutableStateFlow(GlobalSyncState.AllSynced)
        every { sync.unsyncedItems } returns MutableStateFlow(emptyList())
    }
    @Test fun dashboardShowsProductDebtAndRecentSaleWithCorrectNavigation() {
        state.value=HomeUiState(todayTotal=300.0,todayPaid=200.0,todayUnpaid=100.0,unreadChatCount=101,
            recentTransactions=listOf(Transaction(id=41,customerId=7,customerName="فاتورة حديثة",amount=300.0,isPaid=false)),
            dashboard=DashboardUiState(300.0,1,100.0,listOf(TopSellingProduct("p","قهوة",2.5)),
                listOf(TopDebtorCustomer(7,"عميل مدين",100.0)),listOf(DailySales(1700000000000,100.0),DailySales(1700086400000,300.0))))
        var customer=0L;var tx=0L;var all=0;var add=0
        show { HomeScreen({all++},{},{},{add++},{tx=it},{customer=it},vm,sync) }
        ui.onNodeWithText("99+").assertExists()
        ui.onNodeWithText("عملية جديدة",useUnmergedTree=true).performClick();assertEquals(1,add)
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("قهوة"));ui.onNodeWithText("قهوة").assertExists()
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("عميل مدين"));ui.onNodeWithText("عميل مدين").performClick();assertEquals(7,customer)
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("فاتورة حديثة"));ui.onNodeWithText("فاتورة حديثة").performClick();assertEquals(41,tx)
        ui.onNodeWithText("عرض الكل").performClick();assertEquals(1,all)
    }
    @Test fun emptyDashboardDoesNotInventRankingsOrRecentSales() {
        show { HomeScreen({},{},{},{},{},{},vm,sync) }
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("الأصناف الأكثر مبيعاً"))
        ui.onNodeWithText("الأصناف الأكثر مبيعاً").assertExists()
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("أعلى الزبائن مديونية"))
        ui.onNodeWithText("أعلى الزبائن مديونية").assertExists()
        ui.onNodeWithText("آخر العمليات").assertDoesNotExist()
    }
}
