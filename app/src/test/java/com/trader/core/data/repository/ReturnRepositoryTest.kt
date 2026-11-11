package com.trader.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReturnRepositoryTest {
    private lateinit var db:AppDatabase
    private val sync=mockk<FirebaseSyncService>()
    private val activation=mockk<ActivationRepository>()
    private val stock=mockk<StockRepository>()
    private val transactions=mockk<TransactionRepository>()
    private val invoices=mockk<InvoiceItemRepository>()
    private val code=MutableStateFlow("m")
    private val remote=MutableSharedFlow<List<Pair<ReturnInvoice,List<ReturnItem>>>>()
    private fun invoice(id:String="r",amount:Double=25.0)=ReturnInvoice(id=id,originalTransactionId=8,merchantId="m",totalRefund=amount,createdAt=1)
    private fun item(id:String="ri",qty:Double=1.0)=ReturnItem(id=id,returnInvoiceId="r",productId="p",unitId="u",originalQuantity=4.0,returnedQuantity=qty,pricePerUnit=25.0)
    private fun TestScope.repo()=ReturnRepositoryImpl(db,stock,transactions,invoices,"m",activation,sync,backgroundScope)
    private suspend fun awaitRows(predicate:(List<ReturnInvoiceEntity>)->Boolean) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.withTimeout(5000) { db.returnDao().getAllReturns("m").first(predicate) }
        }
    @Before fun open() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        every { activation.observeMerchantCode() } returns code
        coEvery { activation.getMerchantCode() } answers { code.value }
        every { sync.observeReturnInvoices(any()) } returns remote
        coEvery { sync.pushReturnInvoice(any(),any(),any()) } throws IOException("offline fixture")
        // Block the baseline implementation's direct Firebase call as well.
        mockkStatic(FirebaseDatabase::class)
        every { FirebaseDatabase.getInstance() } throws IllegalStateException("External Firebase forbidden in tests")
        coEvery { transactions.getTransactionById(any()) } coAnswers { db.transactionDao().getTransactionById(firstArg())?.toDomain() }
        coEvery { transactions.updateTransaction(any()) } coAnswers {
            val t=firstArg<Transaction>();val old=db.transactionDao().getTransactionById(t.id)!!
            db.transactionDao().updateTransaction(TransactionEntity.fromDomain(t))
            db.cashBoxDao().applyDelta("box",t.amount-old.amount,0)
        }
        coEvery { invoices.getItemsForTransactionOnce(any()) } coAnswers { db.invoiceItemDao().getForTransactionOnce(firstArg()).map { it.toDomain() } }
        coEvery { stock.returnStock(any(),any(),any(),any(),any(),any()) } coAnswers {
            val u=secondArg<String>();db.productDao().updateQuantity(u,db.productDao().getQuantity(u)!!+thirdArg<Double>())
        }
    }
    @After fun close() { db.close();unmockkStatic(FirebaseDatabase::class) }
    private suspend fun seedSale() {
        db.customerDao().upsertCustomer(CustomerEntity(id=7,name="fixture",createdAt=0))
        db.productDao().insertProduct(Product(id="p",merchantId="m").toEntity())
        db.productDao().insertUnits(listOf(ProductUnit(id="u",productId="p",quantityInStock=3.0).toEntity()))
        db.transactionDao().insertTransaction(TransactionEntity.fromDomain(Transaction(id=8,customerId=7,amount=100.0,isPaid=true,hasItems=true,date=0)))
        db.invoiceItemDao().insertInvoiceItem(InvoiceItem(id="line",transactionId=8,productId="p",unitId="u",quantity=4.0,pricePerUnit=25.0,totalPrice=100.0,merchantId="m").toEntity())
        db.cashBoxDao().upsert(CashBoxEntity(id="box",paymentMethodId=1,paymentMethodName="fixture",currentBalance=100.0,initialBalance=100.0,initialBalanceSetAt=0,merchantId="m",updatedAt=0,syncStatus="PENDING"))
    }
    @Test fun emptyRemoteSnapshotPreservesPendingFailedAndConflictingReturns()=runTest {
        val repo=repo();runCurrent()
        for(s in SyncStatus.entries) db.returnDao().insertReturnWithItems(invoice(s.name).copy(syncStatus=s).toEntity(),emptyList())
        remote.emit(emptyList());runCurrent()
        val expected=setOf("PENDING","FAILED","CONFLICT")
        val completed=awaitRows { rows -> rows.map { it.id }.toSet()==expected }
        assertEquals(expected,completed.map { it.id }.toSet())
    }
    @Test fun staleRemoteUpdateDoesNotOverwritePendingRefundOrItsItems()=runTest {
        repo();runCurrent()
        db.returnDao().insertReturnWithItems(invoice().toEntity(),listOf(item().toEntity()))
        remote.emit(listOf(invoice(amount=99.0) to listOf(item(qty=3.0)),invoice("sentinel") to emptyList()));runCurrent()
        awaitRows { rows -> rows.any { it.id=="sentinel" } }
        assertEquals(25.0,db.returnDao().getById("r")!!.totalRefund,0.0)
        assertEquals(1.0,db.returnDao().getReturnItems("r").single().returnedQuantity,0.0)
    }
    @Test fun logoutCancelsPreviousMerchantListener()=runTest {
        repo();runCurrent();assertEquals(1,remote.subscriptionCount.value)
        code.value="";runCurrent()
        assertEquals(0,remote.subscriptionCount.value)
    }
    @Test fun sameReturnIdCannotApplyRefundAndStockTwice()=runTest {
        seedSale();val repo=repo();runCurrent()
        repo.processReturn(invoice(),listOf(item()));repo.processReturn(invoice(),listOf(item()));runCurrent()
        assertEquals(75.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
        assertEquals(75.0,db.cashBoxDao().getById("box")!!.currentBalance,0.0)
        assertEquals(4.0,db.productDao().getQuantity("u")!!,0.0)
        assertEquals(1,db.returnDao().getAllInvoicesOnce().size)
        coVerify(exactly=1) { stock.returnStock(any(),any(),any(),any(),any(),any()) }
    }
    @Test fun duplicateIdWithDifferentPayloadIsRejectedWithoutChangingBalances()=runTest {
        seedSale();val repo=repo();runCurrent();repo.processReturn(invoice(),listOf(item()))
        try { repo.processReturn(invoice(amount=50.0),listOf(item(qty=2.0)));fail("Conflicting duplicate accepted") } catch(expected:IllegalArgumentException) { }
        assertEquals(75.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
        assertEquals(4.0,db.productDao().getQuantity("u")!!,0.0)
    }
    @Test fun aggregateQuantityCannotExceedSoldStockInOneBatch()=runTest {
        seedSale();val repo=repo();runCurrent()
        try { repo.processReturn(invoice(amount=150.0),listOf(item("a",3.0),item("b",3.0)));fail("Over-return accepted") } catch(expected:IllegalArgumentException) { }
        assertTrue(db.returnDao().getAllInvoicesOnce().isEmpty())
        assertEquals(100.0,db.cashBoxDao().getById("box")!!.currentBalance,0.0)
    }
    @Test fun stockFailureRollsBackReturnLedgerAndFinancialChanges()=runTest {
        seedSale();val repo=repo();runCurrent()
        coEvery { stock.returnStock(any(),any(),any(),any(),any(),any()) } throws IOException("injected stock failure")
        try { repo.processReturn(invoice(),listOf(item()));fail("Stock failure swallowed") } catch(expected:IOException) { }
        assertTrue(db.returnDao().getAllInvoicesOnce().isEmpty())
        assertEquals(100.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
    }
    @Test fun successfulPartialReturnMaintainsCashStockInvoiceAndReturnSummary()=runTest {
        seedSale();val repo=repo();runCurrent();repo.processReturn(invoice(),listOf(item()))
        val summary=repo.getReturnSummary(8,db.invoiceItemDao().getForTransactionOnce(8).map { it.toDomain() })
        assertEquals(TransactionReturnStatus.PARTIALLY_RETURNED,summary.returnStatus)
        assertEquals(25.0,summary.totalRefunded,0.0)
        assertEquals(100.0,db.transactionDao().getTransactionById(8)!!.toDomain().originalAmount,0.0)
        assertEquals(1,db.invoiceItemDao().getForTransactionOnce(8).size)
    }

    @Test fun failedUploadRetriesSameDurableIdWithoutReplayingCashOrStock()=runTest {
        seedSale();val repo=repo();runCurrent();repo.processReturn(invoice(),listOf(item()));runCurrent()
        awaitRows { rows -> rows.any { it.id=="r" && it.syncStatus==SyncStatus.FAILED.name } }
        assertEquals("FAILED",db.returnDao().getById("r")!!.syncStatus)
        coEvery { sync.pushReturnInvoice(any(),any(),any()) } just Runs
        repo.syncPendingReturns()
        assertEquals("SYNCED",db.returnDao().getById("r")!!.syncStatus)
        assertEquals(75.0,db.cashBoxDao().getById("box")!!.currentBalance,0.0)
        assertEquals(4.0,db.productDao().getQuantity("u")!!,0.0)
        coVerify(exactly=1) { stock.returnStock(any(),any(),any(),any(),any(),any()) }
    }
    @Test fun retryNeverPushesAnotherMerchantOrConflict()=runTest {
        val repo=repo();runCurrent()
        db.returnDao().insertReturnWithItems(invoice().copy(merchantId="other").toEntity(),emptyList())
        db.returnDao().insertReturnWithItems(invoice("conflict").copy(syncStatus=SyncStatus.CONFLICT).toEntity(),emptyList())
        repo.syncPendingReturns()
        coVerify(exactly=0) { sync.pushReturnInvoice(any(),any(),any()) }
        code.value="other";runCurrent();repo.syncPendingReturns()
        coVerify(exactly=0) { sync.pushReturnInvoice(any(),any(),any()) }
    }
    @Test fun concurrentRetriesSerializeOneAcknowledgement()=runTest {
        val repo=repo();runCurrent();db.returnDao().insertReturnWithItems(invoice().toEntity(),emptyList())
        coEvery { sync.pushReturnInvoice(any(),any(),any()) } coAnswers { kotlinx.coroutines.delay(10);Unit }
        val a=async { repo.syncPendingReturns() }
        val b=async { repo.syncPendingReturns() }
        a.await();b.await()
        coVerify(exactly=1) { sync.pushReturnInvoice(any(),any(),any()) }
        assertEquals("SYNCED",db.returnDao().getById("r")!!.syncStatus)
    }
    @Test fun cancellationLeavesReturnUnacknowledgedAndRetryable()=runTest {
        val repo=repo();runCurrent();db.returnDao().insertReturnWithItems(invoice().toEntity(),emptyList())
        coEvery { sync.pushReturnInvoice(any(),any(),any()) } throws kotlinx.coroutines.CancellationException("fixture cancellation")
        try { repo.syncPendingReturns();fail("Cancellation swallowed") } catch(expected:kotlinx.coroutines.CancellationException) { }
        assertEquals("PENDING",db.returnDao().getById("r")!!.syncStatus)
    }
    @Test fun processInterruptionBeforeUploadRetainsReturnForNextRepository()=runTest {
        seedSale()
        val scope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+StandardTestDispatcher(testScheduler))
        val first=ReturnRepositoryImpl(db,stock,transactions,invoices,"m",activation,sync,scope)
        first.processReturn(invoice(),listOf(item()))
        scope.coroutineContext[kotlinx.coroutines.Job]!!.cancel()
        coEvery { sync.pushReturnInvoice(any(),any(),any()) } just Runs
        repo().syncPendingReturns()
        assertEquals("SYNCED",db.returnDao().getById("r")!!.syncStatus)
        assertEquals(75.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
        assertEquals(4.0,db.productDao().getQuantity("u")!!,0.0)
    }

    @Test fun fractionalReturnsConserveCashAndStockAtFloatingPointBoundary()=runTest {
        seedSale();val repo=repo();runCurrent()
        db.transactionDao().updateTransaction(TransactionEntity.fromDomain(Transaction(id=8,customerId=7,amount=30.0,isPaid=true,date=0)))
        db.cashBoxDao().updateBalance("box",30.0,0)
        db.invoiceItemDao().insertInvoiceItem(InvoiceItem(id="line",transactionId=8,productId="p",unitId="u",quantity=0.3,pricePerUnit=100.0,totalPrice=30.0).toEntity())
        repo.processReturn(invoice(amount=10.0),listOf(ReturnItem(id="first",returnInvoiceId="r",productId="p",unitId="u",originalQuantity=0.3,returnedQuantity=0.1,pricePerUnit=100.0)))
        repo.processReturn(invoice("r2",20.0),listOf(ReturnItem(id="second",returnInvoiceId="r2",productId="p",unitId="u",originalQuantity=0.3,returnedQuantity=0.2,pricePerUnit=100.0)))
        assertEquals(0.0,db.cashBoxDao().getById("box")!!.currentBalance,0.0)
        assertEquals(0.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
        assertEquals(3.3,db.productDao().getQuantity("u")!!,0.000000001)
        assertEquals(2,db.returnDao().getAllInvoicesOnce().size)
    }
}
