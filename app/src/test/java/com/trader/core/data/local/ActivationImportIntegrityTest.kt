package com.trader.core.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.*
import com.trader.core.data.repository.ActivationRepositoryImpl
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ActivationImportIntegrityTest {
    private lateinit var db:AppDatabase
    private var queryHook: ((String) -> Unit)? = null
    private lateinit var repo:ActivationRepositoryImpl
    private val sync=mockk<FirebaseSyncService>(relaxed=true)
    private val products=mockk<ProductFirestoreService>(relaxed=true)
    private val remote=MerchantData(listOf(Customer(1,"remote",createdAt=1)),
        listOf(Transaction(id=8,customerId=1,amount=50.0,isPaid=false,date=1)),emptyList(),
        cashBoxes=listOf(CashBox("1",1,"cash",currentBalance=50.0,merchantId="fixture",updatedAt=1)))
    @Before fun setup()=runBlocking {
        mockkStatic(FirebaseDatabase::class)
        every { FirebaseDatabase.getInstance() } returns mockk(relaxed=true)
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries()
            .setQueryCallback({ sql, _ -> queryHook?.invoke(sql) },java.util.concurrent.Executor { it.run() }).build()
        repo=ActivationRepositoryImpl(ApplicationProvider.getApplicationContext(),sync,db.customerDao(),db.transactionDao(),
            db.paymentMethodDao(),db.productDao(),products,db.returnDao(),db.invoiceItemDao(),db.cashBoxDao(),db.cashBoxMovementDao(),db)
        repo.deactivate()
        db.customerDao().insertCustomer(CustomerEntity(id=1,name="local",createdAt=1))
        db.transactionDao().insertTransaction(TransactionEntity.fromDomain(Transaction(id=8,customerId=1,amount=75.0,isPaid=false,date=1)))
        coEvery { sync.fetchAllData("fixture") } returns remote
        coEvery { products.fetchAllProducts(any()) } returns emptyList()
        Unit
    }
    @After fun close()=runBlocking { repo.deactivate();db.close();unmockkStatic(FirebaseDatabase::class) }
    private fun snapshot()=listOf("customers","transactions","cash_boxes","cash_box_movements","invoice_items","sale_outbox","sale_operations","products","product_units","return_invoices","return_items").associateWith { table ->
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY rowid").use { c ->
            buildList { while(c.moveToNext()) add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) }) }
        }
    }
    private suspend fun lateImport(logout:Boolean)=coroutineScope {
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        coEvery { sync.fetchAllData("fixture") } coAnswers { entered.complete(Unit);release.await();remote }
        val task=async(Dispatchers.Default) { repo.saveActivationStatus(true,"fixture") }
        try {
            withTimeout(10000) { entered.await() }
            if(logout) repo.deactivate() else db.saleOutboxDao().insert(SaleOutboxEntity(eventId="event",operationId="operation",merchantId="fixture",aggregateId=8,aggregateRevision=1,eventType="EDIT",payloadVersion=1,payload="{}",createdAt=1,updatedAt=1))
            val before=snapshot();release.complete(Unit);withTimeout(10000) { task.await() }
            assertEquals("Late import overwrote newer local state",before,snapshot())
        } finally { release.complete(Unit);task.cancelAndJoin() }
    }
    @Test fun managedOperationCommittedDuringFetchBlocksLateImport()=runBlocking { lateImport(false) }
    @Test fun logoutDuringFetchBlocksLateImport()=runBlocking { lateImport(true) }
    @Test fun localImportFailureRollsBackEveryEarlierWrite()=runBlocking {
        val before=snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER import_fault BEFORE INSERT ON cash_boxes BEGIN SELECT RAISE(ABORT,'import failure'); END")
        repo.saveActivationStatus(true,"fixture")
        assertEquals("Import partially committed",before,snapshot())
    }
    @Test fun successfulImportPreservesExpectedBalancesAndDerivedDebt()=runBlocking {
        repo.saveActivationStatus(true,"fixture")
        assertEquals(50.0,db.transactionDao().getTransactionById(8)!!.amount,0.0)
        assertEquals(50.0,db.transactionDao().getUnpaidAmountByCustomer(1),0.0)
        assertEquals(50.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
    }
    private val catalogProduct=Product(id="p",name="fixture",merchantId="fixture",createdAt=1,updatedAt=1)
    private val catalogUnit=ProductUnit(id="u",productId="p",price=25.0,quantityInStock=10.0,createdAt=1,updatedAt=1)
    private fun catalog() {
        coEvery { products.fetchAllProducts("fixture") } returns listOf(catalogProduct)
        coEvery { products.fetchUnitsForProduct("fixture","p") } returns listOf(catalogUnit)
    }
    @Test fun failedUnitDownloadLeavesAllLocalRowsUnchanged()=runBlocking {
        catalog();val before=snapshot()
        coEvery { products.fetchUnitsForProduct("fixture","p") } throws java.io.IOException("offline fixture")
        repo.saveActivationStatus(true,"fixture")
        assertEquals(before,snapshot())
    }
    @Test fun unitInsertFailureRollsBackCustomerTransactionAndProductWrites()=runBlocking {
        catalog();val before=snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER unit_fault BEFORE INSERT ON product_units BEGIN SELECT RAISE(ABORT,'unit failure'); END")
        repo.saveActivationStatus(true,"fixture")
        assertEquals(before,snapshot())
        assertEquals(75.0,db.transactionDao().getUnpaidAmountByCustomer(1),0.0)
    }
    @Test fun successfulCatalogImportDoesNetworkOutsideRoomAndPreservesStock()=runBlocking {
        coEvery { sync.fetchAllData("fixture") } coAnswers {
            assertFalse("Merchant download holds a Room transaction",db.inTransaction());remote
        }
        coEvery { products.fetchAllProducts("fixture") } coAnswers {
            assertFalse("Catalog download holds a Room transaction",db.inTransaction());listOf(catalogProduct)
        }
        coEvery { products.fetchUnitsForProduct("fixture","p") } coAnswers {
            assertFalse("Unit download holds a Room transaction",db.inTransaction());listOf(catalogUnit)
        }
        repo.saveActivationStatus(true,"fixture")
        assertEquals(10.0,db.productDao().getQuantity("u")!!,0.0)
        assertEquals("fixture",db.productDao().getById("p")!!.product.merchantId)
        assertEquals(50.0,db.transactionDao().getUnpaidAmountByCustomer(1),0.0)
        assertEquals(50.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
    }
    @Test fun accountSwitchDuringFetchCannotApplyPreviousMerchantReply()=runBlocking {
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        coEvery { sync.fetchAllData("fixture") } coAnswers { entered.complete(Unit);release.await();remote }
        coEvery { sync.fetchAllData("other") } returns MerchantData(listOf(Customer(2,"other",createdAt=1)),emptyList(),emptyList())
        val task=async(Dispatchers.Default) { repo.saveActivationStatus(true,"fixture") }
        try {
            withTimeout(10000) { entered.await() }
            repo.saveActivationStatus(true,"other");val before=snapshot()
            release.complete(Unit);withTimeout(10000) { task.await() }
            assertEquals("other",repo.getMerchantCode());assertEquals(before,snapshot())
        } finally { release.complete(Unit);task.cancelAndJoin() }
    }
    @Test fun cancelledUnitDownloadDoesNotPersistAnyStagedRows()=runBlocking {
        catalog();val before=snapshot();val entered=CompletableDeferred<Unit>()
        coEvery { products.fetchUnitsForProduct("fixture","p") } coAnswers { entered.complete(Unit);awaitCancellation() }
        val task=launch(Dispatchers.Default) { repo.saveActivationStatus(true,"fixture") }
        try {
            withTimeout(10000) { entered.await() };task.cancelAndJoin()
            assertTrue(task.isCancelled);assertEquals(before,snapshot())
        } finally { task.cancelAndJoin() }
    }
    @Test fun cancellationDuringCashInsertRollsBackEveryImportedRow()=runBlocking {
        catalog();val before=snapshot();var reached=false;var target:Job?=null
        queryHook={ sql ->
            if(sql.contains("cash_boxes",ignoreCase=true) && sql.trimStart().startsWith("INSERT",ignoreCase=true)) {
                reached=true;target!!.cancel()
            }
        }
        val ready=CompletableDeferred<Unit>()
        val task=launch(Dispatchers.Default) { ready.await();repo.saveActivationStatus(true,"fixture") }
        target=task;ready.complete(Unit)
        try {
            withTimeout(10000) { task.join() }
            assertTrue("Cash insert cancellation point not reached",reached);assertTrue(task.isCancelled)
            assertEquals(before,snapshot())
        } finally { queryHook=null;task.cancelAndJoin() }
    }

}
