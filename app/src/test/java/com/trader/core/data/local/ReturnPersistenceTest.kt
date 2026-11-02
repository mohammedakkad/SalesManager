package com.trader.core.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ReturnPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var db: AppDatabase? = null
    private val name = "return-restart-test.db"
    private fun open(): AppDatabase = Room.databaseBuilder(context,AppDatabase::class.java,name)
        .allowMainThreadQueries().build().also { db=it }
    @After fun cleanup() { db?.close(); context.deleteDatabase(name) }

    @Test fun partialReturnSurvivesDatabaseCloseAndReopenWithoutChangingDebt() = runTest {
        val first=open()
        first.customerDao().upsertCustomer(CustomerEntity(id=7,name="fixture",createdAt=0))
        val tx=Transaction(id=8,customerId=7,amount=75.0,originalAmount=100.0,isPaid=false,
            hasItems=true,date=0,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED)
        first.transactionDao().insertTransaction(TransactionEntity.fromDomain(tx))
        first.invoiceItemDao().insertInvoiceItem(InvoiceItem(id="line",transactionId=8,quantity=4.0,pricePerUnit=25.0,totalPrice=100.0).toEntity())
        first.returnDao().insertReturnWithItems(ReturnInvoice(id="return",originalTransactionId=8,totalRefund=25.0).toEntity(),
            listOf(ReturnItem(id="returned-line",returnInvoiceId="return",unitId="unit",originalQuantity=4.0,returnedQuantity=1.0,pricePerUnit=25.0).toEntity()))
        first.close()
        val reopened=open()
        val restored=reopened.transactionDao().getTransactionById(8)!!.toDomain()
        assertEquals(100.0,restored.originalAmount,0.0)
        assertEquals(TransactionReturnStatus.PARTIALLY_RETURNED,restored.returnStatus)
        assertEquals(75.0,reopened.transactionDao().getUnpaidAmountByCustomer(7),0.0)
        assertEquals(1,reopened.invoiceItemDao().getForTransactionOnce(8).size)
        assertEquals(1.0,reopened.returnDao().totalReturnedForUnit(8,"unit"),0.0)
    }
    @Test fun fullReturnSurvivesRestartWithOriginalInvoiceValue() = runTest {
        val first=open()
        first.customerDao().upsertCustomer(CustomerEntity(id=7,name="fixture",createdAt=0))
        first.transactionDao().insertTransaction(TransactionEntity.fromDomain(Transaction(id=8,customerId=7,
            amount=0.0,originalAmount=100.0,isPaid=true,date=0,returnStatus=TransactionReturnStatus.FULLY_RETURNED)))
        first.close()
        val restored=open().transactionDao().getTransactionById(8)!!.toDomain()
        assertEquals(100.0,restored.originalAmount,0.0)
        assertTrue(restored.isFullyReturned)
    }
}
