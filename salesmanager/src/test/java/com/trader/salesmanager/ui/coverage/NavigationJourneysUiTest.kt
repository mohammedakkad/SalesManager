package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.ui.activation.*
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.ui.debts.*
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.navigation.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Real NavHost/route wiring, with offline screen models, not mocked navigation. */
@RunWith(RobolectricTestRunner::class)
class NavigationJourneysUiTest:OfflineSellerUiTest() {
    private val activation=mockk<ActivationViewModel>(relaxed=true)
    private val startup=MutableStateFlow<StartupState>(StartupState.Proceed)
    private val watcher=mockk<MerchantWatcherViewModel>(relaxed=true)
    private val events=MutableSharedFlow<MerchantEvent>(extraBufferCapacity=1)
    @Before fun models() {
        every{activation.startupState} returns startup;every{activation.uiState} returns MutableStateFlow(ActivationUiState())
        every{watcher.event} returns events;every{watcher.expiryBanner} returns MutableStateFlow(null)
        val home=mockk<HomeViewModel>(relaxed=true);every{home.uiState} returns MutableStateFlow(HomeUiState())
        val sync=mockk<SyncStatusViewModel>(relaxed=true);every{sync.syncState} returns MutableStateFlow(GlobalSyncState.AllSynced);every{sync.unsyncedItems} returns MutableStateFlow(emptyList())
        val customers=mockk<CustomersViewModel>(relaxed=true);every{customers.uiState} returns MutableStateFlow(CustomersUiState());every{customers.deleteConfirm} returns MutableStateFlow(null)
        val stock=mockk<InventoryListViewModel>(relaxed=true);every{stock.uiState} returns MutableStateFlow(InventoryListUiState())
        val debts=mockk<DebtsViewModel>(relaxed=true);every{debts.uiState} returns MutableStateFlow(DebtsUiState(isLoading=false))
        val reports=mockk<ReportsViewModel>(relaxed=true);every{reports.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false))
        loadKoinModules(module {
            viewModel {activation};viewModel {watcher};viewModel {home};viewModel {sync};viewModel {customers};viewModel {stock};viewModel {debts};viewModel {reports}
        })
    }
    @Test fun mainTabsNavigateAndReselectionDoesNotDuplicateDestination() {
        show{AppNavigation()}
        ui.onNodeWithText("مدير المبيعات").assertExists()
        for(label in listOf("الزبائن","المخزن","الديون","التقارير","الرئيسية")) {
            ui.onNode(hasText(label) and isSelectable()).performClick()
            ui.onNode(hasText(label) and isSelectable()).assertIsSelected()
            ui.onNode(hasText(label) and isSelectable()).performClick();ui.onNode(hasText(label) and isSelectable()).assertIsSelected()
        }
        ui.onNodeWithText("مدير المبيعات").assertExists()
    }
    @Test fun startupCheckingThenBlockedDoesNotExposeMainTabs() {
        startup.value=StartupState.Checking;show{AppNavigation()}
        ui.onNodeWithText("جاري التحقق من الحساب...").assertExists();ui.onNodeWithContentDescription("المخزن").assertDoesNotExist()
        ui.runOnIdle{startup.value=StartupState.Blocked("حساب معطل",true)}
        ui.onNodeWithText("حساب معطل").assertExists();ui.onNodeWithText("إعادة المحاولة").performClick();verify{activation.checkStartup()}
    }
    @Test fun merchantDisabledEventRequiresAcknowledgmentThenRoutesToActivation() {
        show{AppNavigation()};ui.runOnIdle{assertTrue(events.tryEmit(MerchantEvent.Disabled))}
        ui.onNodeWithText("تم تعطيل حسابك من قِبل الإدارة.").assertExists()
        ui.onNodeWithText("حسناً").performClick();verify{activation.deactivate()}
        ui.onNodeWithText("كود التفعيل").assertExists();ui.onNodeWithContentDescription("المخزن").assertDoesNotExist()
    }
}
