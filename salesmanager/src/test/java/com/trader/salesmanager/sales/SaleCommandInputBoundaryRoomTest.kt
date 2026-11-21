package com.trader.salesmanager.sales

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Public command validation against actual SQLite; each rejected operation must preserve every ledger. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SaleCommandInputBoundaryRoomTest {
    private lateinit var db:AppDatabase
    private val merchant="boundary-merchant"
    private val sale=Transaction(customerId=1,amount=50.0,isPaid=true,paymentMethodId=1,date=10)
    private val line=InvoiceItem("line",productId="p",unitId="u",quantity=2.0,pricePerUnit=25.0,totalPrice=50.0)
    private val commands get()=SaleCommands(db)
    @Before fun seed()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        db.customerDao().insertCustomer(CustomerEntity(id=1,name="boundary customer",createdAt=0))
        db.productDao().insertProduct(ProductEntity("p",null,"boundary product","",null,merchant,0,0,"SYNCED"))
        db.productDao().insertUnit(ProductUnitEntity("u","p","PIECE","piece",25.0,quantityInStock=10.0,itemsPerCarton=null,lowStockThreshold=0.0,isDefault=true,createdAt=0,updatedAt=0,syncStatus="SYNCED"))
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"cash",currentBalance=100.0,merchantId=merchant,updatedAt=0))
    }
    @After fun close(){db.close()}
    private fun snapshot()=listOf("transactions","invoice_items","product_units","stock_movements","cash_boxes","cash_box_movements","return_invoices","return_items","sale_operations","sale_outbox").associateWith { table ->
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY rowid").use{c->buildList{while(c.moveToNext())add((0 until c.columnCount).map{if(c.isNull(it))null else c.getString(it)})}}
    }
    private suspend fun rejects(expected:Class<out Exception>,block:suspend ()->Unit){
        val before=snapshot()
        try{block();fail("Expected ${expected.simpleName}")}catch(actual:Exception){assertTrue("Wrong error: $actual",expected.isInstance(actual))}
        assertEquals("All business rows, receipts and Outbox entries must remain unchanged",before,snapshot())
    }
    private suspend fun original():Transaction {
        val id=commands.create("original",merchant,sale,listOf(line))
        return db.transactionDao().getTransactionById(id)!!.toDomain()
    }
    @Test fun creationRequiresANewTransactionIdentity()=runBlocking {
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale.copy(id=90),listOf(line))}
    }
    @Test fun creationRejectsNonpositiveAndNonfiniteGrossAmounts()=runBlocking {
        for(amount in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY))
            rejects(IllegalArgumentException::class.java){commands.create("bad-gross",merchant,sale.copy(amount=amount),emptyList())}
    }
    @Test fun blankOperationIdentityCannotCreateAnyBusinessOrOutboxRows()=runBlocking {
        rejects(IllegalArgumentException::class.java){commands.create(" \t",merchant,sale,listOf(line))}
    }
    @Test fun blankMerchantCannotWriteTheFinancialAggregate()=runBlocking {
        rejects(IllegalArgumentException::class.java){commands.create("new"," \t",sale,listOf(line))}
    }
    @Test fun invalidInvoiceLineFieldsAreRejectedWithoutPartialFinancialEffects()=runBlocking {
        val invalid=listOf(line.copy(id=""),line.copy(quantity=0.0,totalPrice=0.0),line.copy(quantity=Double.NaN),line.copy(quantity=Double.POSITIVE_INFINITY),line.copy(pricePerUnit=-1.0,totalPrice=-2.0),line.copy(pricePerUnit=Double.NaN),line.copy(pricePerUnit=Double.POSITIVE_INFINITY),line.copy(totalPrice=Double.NaN),line.copy(totalPrice=Double.POSITIVE_INFINITY),line.copy(totalPrice=49.0))
        for(value in invalid)rejects(IllegalArgumentException::class.java){commands.create("invalid-line",merchant,sale,listOf(value))}
    }
    @Test fun duplicateLineIdsCannotCollapseTwoSoldQuantitiesIntoOneStoredRow()=runBlocking {
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale.copy(amount=100.0),listOf(line,line))}
    }
    @Test fun invoiceLineTotalsCannotExceedTheGrossSaleAmount()=runBlocking {
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale.copy(amount=49.0),listOf(line))}
    }
    @Test fun aRemovedUnitRollsBackTheAlreadyInsertedSaleAndInvoiceLines()=runBlocking {
        rejects(IllegalStateException::class.java){commands.create("new",merchant,sale,listOf(line.copy(unitId="removed")))}
    }
    @Test fun anotherMerchantsProductCannotBeSoldUnderTheCurrentSession()=runBlocking {
        db.productDao().updateProduct(db.productDao().getById("p")!!.product.copy(merchantId="other"))
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale,listOf(line))}
    }
    @Test fun missingCashProvisioningRollsBackSaleInvoiceAndStockDeduction()=runBlocking {
        db.cashBoxDao().deleteAll()
        rejects(IllegalStateException::class.java){commands.create("new",merchant,sale,listOf(line))}
    }
    @Test fun anotherMerchantsCashBoxCannotReceiveTheSaleProceeds()=runBlocking {
        val box=db.cashBoxDao().getById("1")!!
        db.cashBoxDao().upsert(box.copy(merchantId="other"))
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale,listOf(line))}
    }
    @Test fun finiteInputsWhoseCashAdditionOverflowsAreRolledBack()=runBlocking {
        val box=db.cashBoxDao().getById("1")!!
        db.cashBoxDao().upsert(box.copy(currentBalance=Double.MAX_VALUE))
        rejects(IllegalArgumentException::class.java){commands.create("new",merchant,sale.copy(amount=Double.MAX_VALUE),listOf(line))}
    }
    @Test fun editingAMissingSaleDoesNotInventANewAggregate()=runBlocking {
        rejects(IllegalStateException::class.java){commands.edit("edit",merchant,sale.copy(id=90),sale.copy(id=90),null)}
    }
    @Test fun editingCannotChangeTheExistingTransactionIdentity()=runBlocking {
        val old=original()
        rejects(IllegalArgumentException::class.java){commands.edit("edit",merchant,old,old.copy(id=old.id+1),null)}
    }
    @Test fun editingRejectsNegativeAndNonfiniteGrossAmounts()=runBlocking {
        val old=original()
        for(amount in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY))
            rejects(IllegalArgumentException::class.java){commands.edit("edit",merchant,old,old.copy(amount=amount),null)}
    }
    @Test fun deletionRequiresAPersistedPositiveTransactionId()=runBlocking {
        for(id in listOf(0L,-1L))rejects(IllegalArgumentException::class.java){commands.delete("delete",merchant,sale.copy(id=id))}
    }
}
