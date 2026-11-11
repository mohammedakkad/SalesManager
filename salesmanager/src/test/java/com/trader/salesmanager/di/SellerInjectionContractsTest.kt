package com.trader.salesmanager.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.trader.core.data.local.dao.*
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.sales.SaleCommands
import com.trader.salesmanager.ui.returns.ReturnViewModel
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual production module/DAO/command factories; offline repository fixtures isolate network/lifecycle. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SellerInjectionContractsTest {
    private lateinit var db:AppDatabase
    private lateinit var app:KoinApplication
    private val store=ViewModelStore()
    private val actor=MutableStateFlow("m1")
    private val activation=mockk<ActivationRepository>()
    private val customers=mockk<CustomerRepository>()
    private val transactions=mockk<TransactionRepository>(relaxed=true)
    private val invoices=mockk<InvoiceItemRepository>(relaxed=true)
    private val stock=mockk<StockRepository>(relaxed=true)
    private val products=mockk<ProductRepository>(relaxed=true)
    private val payments=mockk<PaymentMethodRepository>(relaxed=true)
    private val returns=mockk<ReturnRepository>(relaxed=true)
    private val customer=Customer(1,"offline fixture")
    @Before fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context=ApplicationProvider.getApplicationContext<Application>()
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        mockkStatic(FirebaseDatabase::class);mockkStatic(FirebaseFirestore::class)
        every{FirebaseDatabase.getInstance()} throws IllegalStateException("Real Firebase forbidden in offline graph tests")
        every{FirebaseFirestore.getInstance()} throws IllegalStateException("Real Firebase forbidden in offline graph tests")
        coEvery{activation.getMerchantCode()} answers {actor.value};every{activation.observeMerchantCode()} returns actor
        every{customers.getAllCustomers()} returns flowOf(listOf(customer));every{payments.getAllPaymentMethods()} returns flowOf(emptyList())
        every{products.getAllProducts()} returns flowOf(emptyList())
        app=koinApplication {
            androidContext(context)
            modules(salesManagerModule,module {
                single<AppDatabase>{db};single<ActivationRepository>{activation};single<CustomerRepository>{customers}
                single<TransactionRepository>{transactions};single<InvoiceItemRepository>{invoices};single<ProductRepository>{products}
                single<PaymentMethodRepository>{payments};single<StockRepository>{stock};single<ReturnRepository>{returns};single{SavedStateHandle()}
            })
        }
    }
    @After fun cleanup(){store.clear();app.close();db.close();unmockkStatic(FirebaseDatabase::class);unmockkStatic(FirebaseFirestore::class);Dispatchers.resetMain()}
    @Test fun productionDaoFactoriesShareTheSameRoomDatabaseAndCommandSingleton() {
        val k=app.koin
        assertSame(db,k.get<AppDatabase>())
        assertSame(db.customerDao(),k.get<CustomerDao>());assertSame(db.transactionDao(),k.get<TransactionDao>())
        assertSame(db.paymentMethodDao(),k.get<PaymentMethodDao>());assertSame(db.pendingMessageDao(),k.get<PendingMessageDao>())
        assertSame(db.productDao(),k.get<ProductDao>());assertSame(db.stockMovementDao(),k.get<StockMovementDao>())
        assertSame(db.invoiceItemDao(),k.get<InvoiceItemDao>());assertSame(db.inventoryDao(),k.get<InventoryDao>())
        assertSame(db.returnDao(),k.get<ReturnDao>());assertSame(db.sessionDao(),k.get<SessionDao>())
        assertSame(db.employeeDao(),k.get<EmployeeDao>());assertSame(db.cashBoxDao(),k.get<CashBoxDao>())
        assertSame(db.cashBoxMovementDao(),k.get<CashBoxMovementDao>());assertSame(db.lowStockAlertDao(),k.get<LowStockAlertDao>())
        assertSame(k.get<SaleCommands>(),k.get<SaleCommands>())
    }
    @Test fun injectedSaleViewModelUsesTheRealAggregateCommandInsteadOfIndependentRepositoryWrites()=runBlocking {
        db.customerDao().upsertCustomer(CustomerEntity(id=1,name="offline fixture",createdAt=0))
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"cash",currentBalance=100.0,merchantId="m1",updatedAt=0))
        val vm=app.koin.get<AddEditTransactionViewModel>();store.put("sale",vm)
        vm.selectCustomer(customer);vm.updateAmount("50");vm.selectPaymentMethod(PaymentMethod(1,"cash",PaymentType.CASH));vm.save()
        val state=withTimeout(10000){vm.uiState.first{it.isSaved||it.error!=null}}
        assertTrue(state.error.orEmpty(),state.isSaved)
        assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        assertEquals("m1",db.saleOutboxDao().all().single().merchantId)
        coVerify(exactly=0){transactions.insertTransaction(any())};coVerify(exactly=0){invoices.saveItems(any())}
    }
    @Test fun returnFactoryAcceptsTheActualNavigationParametersAndKeepsTheRequestedInvoiceIdentity()=runBlocking {
        val item=InvoiceItem("line",41,"p","fixture","u","piece",3.0,25.0,75.0,"m1")
        coEvery{invoices.getItemsForTransactionOnce(41)} returns listOf(item)
        coEvery{returns.getAlreadyReturnedQty(41,"u")} returns 1.0
        coEvery{products.getProductById("p")} returns null
        val vm=app.koin.get<ReturnViewModel>{parametersOf(41L,Unit)};store.put("return",vm)
        val state=withTimeout(10000){vm.state.first{!it.isLoading}}
        assertEquals(41L,state.lines.single().invoiceItem.transactionId)
        assertEquals(2.0,state.lines.single().maxReturnable,0.0);assertEquals(50.0,state.totalRefund,0.0)
        coVerify(exactly=1){invoices.getItemsForTransactionOnce(41)}
    }
}
