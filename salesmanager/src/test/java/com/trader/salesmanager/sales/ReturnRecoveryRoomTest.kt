package com.trader.salesmanager.sales

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.*
import com.trader.core.data.repository.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.returns.ReturnViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real Room sale/return/cash/stock/outbox pipeline; Firebase collaborators never connect. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ReturnRecoveryRoomTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private lateinit var scope:CoroutineScope
    private lateinit var returns:ReturnRepository
    private lateinit var invoices:InvoiceItemRepository
    private lateinit var tx:Transaction
    private val products=mockk<ProductRepository>()
    private val models=mutableListOf<ReturnViewModel>()
    private val merchant="return-fixture"
    private val name="return-recovery.db"
    private fun open() {db=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build()}
    @Before fun setup()=runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        context.deleteDatabase(name);open();scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        db.customerDao().upsertCustomer(CustomerEntity(id=1,name="fixture",createdAt=0))
        val product=Product("p",name="fixture",merchantId=merchant)
        val unit=ProductUnit("u","p",unitLabel="piece",price=25.0,costPrice=10.0,quantityInStock=4.0)
        db.productDao().insertProduct(product.toEntity());db.productDao().insertUnit(unit.toEntity())
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"cash",currentBalance=100.0,merchantId=merchant,updatedAt=0))
        coEvery{products.getProductById("p")} returns ProductWithUnits(product,listOf(unit))
        val commands=SaleCommands(db)
        val id=commands.create("seed",merchant,Transaction(customerId=1,amount=50.0,isPaid=true,paymentMethodId=1,date=0),
            listOf(InvoiceItem("line",productId="p",unitId="u",productName="fixture",unitLabel="piece",quantity=2.0,pricePerUnit=25.0,totalPrice=50.0)))
        tx=db.transactionDao().getTransactionById(id)!!.toDomain()
        val activation=mockk<ActivationRepository>();coEvery{activation.getMerchantCode()} returns merchant
        every{activation.observeMerchantCode()} returns emptyFlow()
        val sync=mockk<FirebaseSyncService>(relaxed=true)
        val remote=mockk<ProductFirestoreService>(relaxed=true)
        invoices=InvoiceItemRepositoryImpl(db.invoiceItemDao(),remote,sync,merchant,db,activation)
        val stock=StockRepositoryImpl(db.productDao(),db.stockMovementDao(),remote,merchant,db,activation)
        val cash=CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),sync,activation,scope)
        val transactions=TransactionRepositoryImpl(db.transactionDao(),db.customerDao(),db.paymentMethodDao(),sync,activation,invoices,stock,cash,db.returnDao(),scope,db)
        returns=ReturnRepositoryImpl(db,stock,transactions,invoices,merchant,activation,sync,scope,commands::journalReturn)
    }
    @After fun cleanup() {models.forEach{it.viewModelScope.cancel()};scope.cancel();db.close();context.deleteDatabase(name);Dispatchers.resetMain()}
    private suspend fun model(repo:ReturnRepository=returns):ReturnViewModel {
        val vm=ReturnViewModel(repo,invoices,products,merchant,tx.id);models+=vm
        withTimeout(10000){vm.state.first{!it.isLoading}}
        vm.updateQty(0,1.0);return vm
    }
    private suspend fun finished(vm:ReturnViewModel)=withTimeout(10000){vm.state.first{it.processingState!=ReturnUiState.Loading}}
    private suspend fun assertOneRefund() {
        println("RETURN_STATE invoices=${db.returnDao().getAllInvoicesOnce().size} amount=${db.transactionDao().getTransactionById(tx.id)!!.amount} cash=${db.cashBoxDao().getById("1")!!.currentBalance} stock=${db.productDao().getQuantity("u")} events=${db.saleOutboxDao().all().map{it.eventType}}")
        assertEquals(1,db.returnDao().getAllInvoicesOnce().size)
        assertEquals(25.0,db.transactionDao().getTransactionById(tx.id)!!.amount,0.0)
        assertEquals(125.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        assertEquals(3.0,db.productDao().getQuantity("u")!!,0.0)
        assertEquals(listOf("CREATE","RETURN"),db.saleOutboxDao().all().map{it.eventType})
    }
    @Test fun cancellationAfterActualCommitRetriesTheSameRefundWithoutAnotherCashOrStockEffect()=runBlocking {
        var first=true
        val uncertain=mockk<ReturnRepository>()
        coEvery{uncertain.getAlreadyReturnedQty(any(),any())} coAnswers {returns.getAlreadyReturnedQty(firstArg(),secondArg())}
        coEvery{uncertain.processReturn(any(),any())} coAnswers {
            val result=returns.processReturn(firstArg(),secondArg())
            if(first){first=false;throw CancellationException("lost committed result")};result
        }
        val vm=model(uncertain);vm.confirmReturn();finished(vm)
        assertOneRefund();vm.dismissConfirmSheet();vm.confirmReturn();finished(vm)
        assertTrue(vm.state.value.processingState is ReturnUiState.Success);assertOneRefund()
    }
    @Test fun failedOutboxInsertRollsBackTheWholeReturnAndRetryCommitsOnce()=runBlocking {
        val vm=model()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER injected_return BEFORE INSERT ON sale_outbox BEGIN SELECT RAISE(ABORT,'offline injected failure'); END")
        vm.confirmReturn();finished(vm)
        assertTrue(vm.state.value.processingState is ReturnUiState.Error)
        assertTrue(db.returnDao().getAllInvoicesOnce().isEmpty())
        assertEquals(50.0,db.transactionDao().getTransactionById(tx.id)!!.amount,0.0)
        assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        assertEquals(2.0,db.productDao().getQuantity("u")!!,0.0)
        assertEquals(1,db.saleOutboxDao().all().size)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER injected_return")
        vm.dismissConfirmSheet();vm.confirmReturn();finished(vm);assertOneRefund()
    }
    @Test fun committedReturnSurvivesReopenAndFreshDraftUsesOnlyRemainingQuantity()=runBlocking {
        val vm=model();vm.confirmReturn();finished(vm);assertOneRefund()
        vm.viewModelScope.cancel();scope.cancel();db.close();open()
        assertOneRefund()
        assertEquals(1.0,db.returnDao().totalReturnedForUnit(tx.id,"u"),0.0)
        val fresh=mockk<ReturnRepository>()
        coEvery{fresh.getAlreadyReturnedQty(any(),any())} coAnswers {db.returnDao().totalReturnedForUnit(firstArg(),secondArg())}
        val reads=mockk<InvoiceItemRepository>()
        coEvery{reads.getItemsForTransactionOnce(any())} coAnswers {db.invoiceItemDao().getForTransactionOnce(firstArg()).map{it.toDomain()}}
        val next=ReturnViewModel(fresh,reads,products,merchant,tx.id);models+=next
        withTimeout(10000){next.state.first{!it.isLoading}}
        assertEquals(1.0,next.state.value.lines.single().maxReturnable,0.0)
        assertEquals(25.0,next.state.value.totalRefund,0.0)
        assertEquals(listOf("CREATE","RETURN"),db.saleOutboxDao().all().map{it.eventType})
    }
}
