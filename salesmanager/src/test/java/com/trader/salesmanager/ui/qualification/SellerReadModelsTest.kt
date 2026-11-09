package com.trader.salesmanager.ui.qualification

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.core.domain.usecase.GetDashboardAnalyticsUseCase
import com.trader.core.sync.*
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.activation.*
import com.trader.salesmanager.ui.payments.*
import com.trader.salesmanager.ui.inventory.reports.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SellerReadModelsTest {
    private val store=ViewModelStore();private var key=0
    private fun <T:androidx.lifecycle.ViewModel> own(vm:T)=vm.also{store.put("vm${key++}",it)}
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher())}
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    @Test fun homeProjectsLiveAnalyticsRecentSalesAndMerchantUnreadCount()=runTest {
        val tx=mockk<TransactionRepository>();val chat=mockk<ChatRepository>();val activation=mockk<ActivationRepository>();val analytics=mockk<GetDashboardAnalyticsUseCase>()
        val data=MutableStateFlow(DashboardAnalytics(TodaySalesSummary(150.0,2,100.0,50.0),50.0,listOf(TopSellingProduct("p","أرز",3.0)),listOf(TopDebtorCustomer(7,"fixture",50.0)),listOf(DailySales(0,150.0))))
        val recent=MutableStateFlow(listOf(Transaction(41,7,"fixture",150.0,isPaid=true)));val unread=MutableStateFlow(3)
        every{tx.observeRecentTransactions(5)} returns recent;every{analytics.invoke(any())} returns data
        coEvery{activation.getMerchantCode()} returns "m1";every{chat.getUnreadCount("m1","m1")} returns unread
        val vm=own(HomeViewModel(tx,chat,activation,analytics));runCurrent()
        assertEquals(150.0,vm.uiState.value.todayTotal,0.0);assertEquals(50.0,vm.uiState.value.dashboard.totalOutstandingDebt,0.0);assertEquals(41,vm.uiState.value.recentTransactions.single().id);assertEquals(3,vm.uiState.value.unreadChatCount)
        unread.value=4;data.value=data.value.copy(todaySummary=TodaySalesSummary(200.0,3,150.0,50.0));recent.value=emptyList();runCurrent()
        assertEquals(200.0,vm.uiState.value.todayTotal,0.0);assertEquals(4,vm.uiState.value.unreadChatCount);assertTrue(vm.uiState.value.recentTransactions.isEmpty());assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun inactiveHomeDoesNotSubscribeToAnotherMerchantsChat()=runTest {
        val tx=mockk<TransactionRepository>();val chat=mockk<ChatRepository>();val activation=mockk<ActivationRepository>();val analytics=mockk<GetDashboardAnalyticsUseCase>()
        every{tx.observeRecentTransactions(5)} returns flowOf(emptyList());every{analytics.invoke(any())} returns flowOf(DashboardAnalytics());coEvery{activation.getMerchantCode()} returns ""
        val vm=own(HomeViewModel(tx,chat,activation,analytics));runCurrent();assertEquals(0,vm.uiState.value.unreadChatCount);verify(exactly=0){chat.getUnreadCount(any(),any())}
    }
    @Test fun customerSearchDebouncesAndDeleteRequiresCurrentConfirmation()=runTest {
        val repo=mockk<CustomerRepository>(relaxed=true);val customer=Customer(7,"سارة",syncStatus=SyncStatus.PENDING)
        every{repo.getAllCustomers()} returns flowOf(listOf(customer,Customer(8,"آخر",syncStatus=SyncStatus.SYNCED)));every{repo.searchCustomers("سارة")} returns flowOf(listOf(customer));coEvery{repo.getTransactionCount(7)} returns 3
        val vm=own(CustomersViewModel(repo));assertTrue(vm.uiState.value.isLoading);advanceTimeBy(301);runCurrent();assertEquals(2,vm.uiState.value.customers.size);assertEquals(1,vm.uiState.value.pendingSyncCount)
        vm.updateSearch("سا");vm.updateSearch("سارة");advanceTimeBy(301);runCurrent();assertEquals(listOf(customer),vm.uiState.value.customers);verify(exactly=0){repo.searchCustomers("سا")}
        vm.confirmDelete();runCurrent();coVerify(exactly=0){repo.deleteCustomer(any())};vm.requestDelete(customer);runCurrent();assertEquals(3,vm.deleteConfirm.value!!.transactionCount)
        vm.dismissDelete();vm.confirmDelete();runCurrent();coVerify(exactly=0){repo.deleteCustomer(any())}
        vm.requestDelete(customer);runCurrent();vm.confirmDelete();runCurrent();coVerify(exactly=1){repo.deleteCustomer(customer)};assertNull(vm.deleteConfirm.value)
    }
    @Test fun syncModelMirrorsErrorsAndRetryDelegatesToCoordinator()=runTest {
        val observer=mockk<SyncStatusObserver>();val coordinator=mockk<SyncCoordinator>(relaxed=true)
        val state=MutableStateFlow<GlobalSyncState>(GlobalSyncState.AllSynced);val rows=MutableStateFlow<List<UnsyncedItem>>(emptyList())
        every{observer.globalSyncState} returns state;every{observer.unsyncedItems} returns rows
        val vm=own(SyncStatusViewModel(observer,coordinator));runCurrent();state.value=GlobalSyncState.Failed(1);rows.value=listOf(UnsyncedItem("e",UnsyncedItemType.TRANSACTION,"fixture",SyncStatus.FAILED));runCurrent()
        assertEquals(GlobalSyncState.Failed(1),vm.syncState.value);assertEquals("e",vm.unsyncedItems.value.single().id);vm.retrySync();runCurrent();coVerify(exactly=1){coordinator.syncAll()}
    }
    @Test fun merchantWatcherUnsubscribesWhenInactiveAndEmitsExactBlockedReason()=runTest {
        val repo=mockk<ActivationRepository>(relaxed=true);val merchant=MutableStateFlow("");val status=MutableStateFlow<MerchantStatus?>(MerchantStatus.ACTIVE)
        every{repo.observeMerchantCode()} returns merchant;every{repo.observeMerchantStatus()} returns status
        val vm=own(MerchantWatcherViewModel(repo,mockk<Application>()));val events=mutableListOf<MerchantEvent>();backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.event.collect{events+=it}}
        runCurrent();verify(exactly=0){repo.observeMerchantStatus()};merchant.value="m1";runCurrent();assertTrue(events.isEmpty())
        status.value=MerchantStatus.DISABLED;runCurrent();assertEquals(MerchantEvent.Disabled,events.last());status.value=MerchantStatus.EXPIRED;runCurrent();assertEquals(MerchantEvent.Expired,events.last())
        status.value=null;runCurrent();assertEquals(MerchantEvent.Deleted,events.last());coVerify(exactly=3){repo.deactivate()}
        merchant.value="";runCurrent();status.value=MerchantStatus.DISABLED;runCurrent();assertEquals(3,events.size)
    }
    @Test fun paymentMethodsIgnoreBlankInputAndKeepSelectedIdentityForDelete()=runTest {
        val repo=mockk<PaymentMethodRepository>(relaxed=true);val method=PaymentMethod(7,"نقد",PaymentType.CASH);every{repo.getAllPaymentMethods()} returns flowOf(listOf(method))
        val vm=own(PaymentMethodsViewModel(repo));backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()};runCurrent();assertEquals(listOf(method),vm.uiState.value.methods)
        vm.addMethod(" ");vm.addMethod("بطاقة");vm.deleteMethod(method);runCurrent();coVerify(exactly=1){repo.insertPaymentMethod(match{it.name=="بطاقة"})};coVerify{repo.deletePaymentMethod(method)}
    }
    @Test fun stockReportSeparatesPaidCashUnpaidDebtAndActualUnitValue()=runTest {
        val products=mockk<ProductRepository>();val transactions=mockk<TransactionRepository>();val now=System.currentTimeMillis()
        val product=ProductWithUnits(Product(id="p",name="أرز"),listOf(ProductUnit("u","p",unitLabel="كيس",price=20.0,quantityInStock=2.0,lowStockThreshold=3.0)))
        every{products.getAllProducts()} returns flowOf(listOf(product));every{transactions.getAllTransactions()} returns flowOf(listOf(Transaction(1,1,"paid",100.0,isPaid=true,paymentType=PaymentType.CASH,date=now),Transaction(2,1,"unpaid",30.0,isPaid=false,paymentType=PaymentType.DEBT,date=now),Transaction(3,1,"old",50.0,isPaid=true,paymentType=PaymentType.CASH,date=now-40L*86400000)))
        val vm=own(StockReportsViewModel(products,transactions));backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()};runCurrent()
        val state=vm.uiState.value;assertEquals(40.0,state.totalProductsValue,0.0);assertEquals("u",state.stockItems.single().unitId);assertEquals(listOf(product),state.lowStockItems)
        assertEquals(150.0,state.cashSummary.totalCash,0.0);assertEquals(30.0,state.cashSummary.totalDebt,0.0);assertEquals(100.0,state.cashSummary.todayCash,0.0);assertEquals(100.0,state.cashSummary.monthCash,0.0)
    }
}
