package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.core.sync.GlobalSyncState
import com.trader.salesmanager.MainActivity
import com.trader.salesmanager.ui.activation.*
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.debts.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.ui.inventory.invoice.*
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.navigation.AppNavigation
import com.trader.salesmanager.ui.boxes.*
import com.trader.salesmanager.ui.payments.*
import com.trader.salesmanager.ui.chat.*
import com.trader.salesmanager.ui.settings.backup.*
import com.trader.salesmanager.ui.transactions.addedit.*
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Offline dependencies shared by actual NavHost journey tests. */
abstract class OfflineNavigationFixture:OfflineSellerUiTest() {
    protected val activation=mockk<ActivationViewModel>(relaxed=true)
    protected val startup=MutableStateFlow<StartupState>(StartupState.Proceed)
    protected val watcher=mockk<MerchantWatcherViewModel>(relaxed=true)
    protected val events=MutableSharedFlow<MerchantEvent>(extraBufferCapacity=1)
    protected val stock=mockk<InventoryListViewModel>(relaxed=true)
    protected val sale=mockk<AddEditTransactionViewModel>(relaxed=true)
    protected val customer=Customer(7,"زبون مسار")
    protected val saleState=MutableStateFlow(AddEditTransactionUiState(customers=listOf(customer),selectedCustomer=customer,amount="50"))
    protected val lineVm=mockk<InvoiceItemsViewModel>(relaxed=true)
    protected var appliedJson:String?=null
    protected val unit=ProductUnit("u","p",UnitType.WEIGHT,"kg",price=50.0,quantityInStock=100.0)
    protected val line=InvoiceLineItem(product=ProductWithUnits(Product("p",name="أرز المسار"),listOf(unit)),selectedUnit=unit,
        displayQty=2.0,displayWeightUnit=SaleWeightUnit.POUND,customPrice=45.0,stockAvailable=100.0)
    @Before fun models() {
        every{activation.startupState} returns startup;every{activation.uiState} returns MutableStateFlow(ActivationUiState())
        every{watcher.event} returns events;every{watcher.expiryBanner} returns MutableStateFlow(null)
        val home=mockk<HomeViewModel>(relaxed=true);every{home.uiState} returns MutableStateFlow(HomeUiState())
        val sync=mockk<SyncStatusViewModel>(relaxed=true);every{sync.syncState} returns MutableStateFlow(GlobalSyncState.AllSynced);every{sync.unsyncedItems} returns MutableStateFlow(emptyList())
        val customers=mockk<CustomersViewModel>(relaxed=true);every{customers.uiState} returns MutableStateFlow(CustomersUiState(isLoading=false));every{customers.deleteConfirm} returns MutableStateFlow(null)
        every{stock.uiState} returns MutableStateFlow(InventoryListUiState(isLoading=false))
        val debts=mockk<DebtsViewModel>(relaxed=true);every{debts.uiState} returns MutableStateFlow(DebtsUiState(isLoading=false))
        val reports=mockk<ReportsViewModel>(relaxed=true);every{reports.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false))
        val updates=mockk<AppUpdateViewModel>(relaxed=true);every{updates.uiState} returns MutableStateFlow(AppUpdateUiState())
        val low=mockk<LowStockSettingsRepository>(relaxed=true);every{low.settings} returns MutableStateFlow(LowStockAlertSettings())
        val boxes=mockk<BoxesViewModel>(relaxed=true);every{boxes.uiState} returns MutableStateFlow(BoxesUiState(isLoading=false))
        val methods=mockk<PaymentMethodsViewModel>(relaxed=true);every{methods.uiState} returns MutableStateFlow(PaymentMethodsUiState(listOf(PaymentMethod(3,"طريقة المسار"))))
        val backup=mockk<BackupViewModel>(relaxed=true);every{backup.uiState} returns MutableStateFlow(BackupUiState())
        val chat=mockk<ChatViewModel>(relaxed=true);every{chat.uiState} returns MutableStateFlow(ChatUiState(isLoading=false,merchantId="offline"))
        val payments=mockk<PaymentMethodRepository>(relaxed=true)
        every{sale.uiState} returns saleState;every{sale.serializePendingLines()} returns null
        every{sale.applyInvoiceLinesFromJson(any())} answers {appliedJson=firstArg();Unit}
        every{lineVm.uiState} returns MutableStateFlow(InvoiceItemsUiState(lines=listOf(line),isLoading=false))
        loadKoinModules(module {
            viewModel{activation};viewModel{watcher};viewModel{home};viewModel{sync};viewModel{customers};viewModel{stock}
            viewModel{debts};viewModel{reports};viewModel{updates};viewModel{boxes};viewModel{methods};viewModel{backup};viewModel{chat}
            viewModel{sale};viewModel{lineVm};single<LowStockSettingsRepository>{low};single<PaymentMethodRepository>{payments}
        })
    }
}
