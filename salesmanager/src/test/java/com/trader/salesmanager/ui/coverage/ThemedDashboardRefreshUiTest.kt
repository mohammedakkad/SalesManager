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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedDashboardRefreshUiTest:AppThemeUiFixture(){
    private val vm=mockk<HomeViewModel>(relaxed=true)
    private val sync=mockk<SyncStatusViewModel>(relaxed=true)
    private val home=MutableStateFlow(HomeUiState())
    private val status=MutableStateFlow<GlobalSyncState>(GlobalSyncState.AllSynced)
    private val pending=MutableStateFlow(emptyList<UnsyncedItem>())
    @Before fun fixture(){every{vm.uiState} returns home;every{sync.syncState} returns status;every{sync.unsyncedItems} returns pending}
    private fun reveal(text:String){ui.onNode(hasScrollAction()).performScrollToNode(hasText(text))}
    @Test fun recentInvoiceReplacementUpdatesItsNavigationIdentityAndPaymentStatus(){
        home.value=HomeUiState(recentTransactions=listOf(Transaction(41,7,"زبون قديم",100.0,isPaid=false)))
        var selected=0L
        showApp{HomeScreen({},{},{},{},{selected=it},{},vm,sync)}
        reveal("زبون قديم");dark(true)
        ui.runOnIdle{home.value=home.value.copy(recentTransactions=listOf(Transaction(42,8,"زبون جديد",75.0,isPaid=true)))}
        ui.onNodeWithText("زبون قديم").assertDoesNotExist();dark(false)
        reveal("زبون جديد");ui.onNodeWithText("زبون جديد").performClick();assertEquals(42L,selected)
        ui.runOnIdle{home.value=home.value.copy(recentTransactions=emptyList())}
        ui.onNodeWithText("آخر العمليات").assertDoesNotExist()
    }
    @Test fun unreadChatBadgeTracksAllSizeThresholdsAndDisappearsWhenRead(){
        home.value=HomeUiState(unreadChatCount=1);var opened=0
        showApp{HomeScreen({},{},{opened++},{},{},{},vm,sync)}
        ui.onNodeWithText("1").assertExists();dark(true)
        ui.runOnIdle{home.value=home.value.copy(unreadChatCount=10)}
        ui.onNodeWithText("10").assertExists()
        ui.runOnIdle{home.value=home.value.copy(unreadChatCount=100)}
        ui.onNodeWithText("99+").assertExists();ui.onNodeWithText("10").assertDoesNotExist()
        dark(false);ui.runOnIdle{home.value=home.value.copy(unreadChatCount=0)}
        ui.onNodeWithText("99+").assertDoesNotExist();assertEquals(0,opened)
        ui.onNodeWithContentDescription("الدعم الفني").performClick();assertEquals(1,opened)
    }
    @Test fun liveRankingsReplaceBothProductsAndDebtorTargetsAcrossThemeChange(){
        home.value=HomeUiState(dashboard=DashboardUiState(topSellingProducts=listOf(TopSellingProduct("p","صنف قديم",3.0)),topDebtorCustomers=listOf(TopDebtorCustomer(7,"مدين قديم",50.0))))
        var selected=0L
        showApp{HomeScreen({},{},{},{},{},{selected=it},vm,sync)}
        reveal("صنف قديم");dark(true)
        ui.runOnIdle{home.value=home.value.copy(dashboard=home.value.dashboard.copy(topSellingProducts=listOf(TopSellingProduct("q","صنف جديد",5.5)),topDebtorCustomers=listOf(TopDebtorCustomer(9,"مدين جديد",75.0))))}
        reveal("صنف جديد");ui.onNodeWithText("صنف قديم").assertDoesNotExist()
        dark(false);reveal("مدين جديد");ui.onNodeWithText("مدين جديد").performClick();assertEquals(9L,selected)
        ui.onNodeWithText("مدين قديم").assertDoesNotExist()
    }
    @Test fun anOpenSyncSheetReplacesFailedRowsWithTheAcknowledgedStateWithoutRetrying(){
        status.value=GlobalSyncState.Failed(1)
        pending.value=listOf(UnsyncedItem("tx",UnsyncedItemType.TRANSACTION,"فاتورة معلقة",SyncStatus.FAILED))
        showApp{HomeScreen({},{},{},{},{},{},vm,sync)}
        ui.onNodeWithContentDescription("فشل في المزامنة — اضغط للتفاصيل").performClick()
        ui.onNodeWithText("فاتورة معلقة").assertExists();dark(true)
        ui.runOnIdle{pending.value=emptyList();status.value=GlobalSyncState.AllSynced}
        ui.onNodeWithText("كل البيانات محفوظة").assertExists()
        ui.onNodeWithText("فاتورة معلقة").assertDoesNotExist();dark(false)
        verify(exactly=0){sync.retrySync()}
    }
    @Test fun retryUsesCurrentPendingRecordsAndThemeChangesDoNotIssueExtraRetries(){
        status.value=GlobalSyncState.Failed(1)
        pending.value=listOf(UnsyncedItem("old",UnsyncedItemType.CASH_BOX,"صندوق قديم",SyncStatus.FAILED))
        showApp{HomeScreen({},{},{},{},{},{},vm,sync)}
        ui.onNodeWithContentDescription("فشل في المزامنة — اضغط للتفاصيل").performClick();dark(true)
        ui.runOnIdle{pending.value=listOf(UnsyncedItem("new",UnsyncedItemType.STOCK_MOVEMENT,"حركة حالية",SyncStatus.FAILED))}
        ui.onNodeWithText("حركة حالية").assertExists();ui.onNodeWithText("صندوق قديم").assertDoesNotExist()
        dark(false);verify(exactly=0){sync.retrySync()}
        ui.onNodeWithText("إعادة المزامنة الآن").performClick();verify(exactly=1){sync.retrySync()}
    }
}
