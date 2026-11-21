package com.trader.salesmanager.ui.settings.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.dao.CustomerDao
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupIntegrityTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    private fun manager(database:AppDatabase=db)=BackupManager(context,database,"fixture")
    private suspend fun seed() {
        db.customerDao().upsertCustomer(CustomerEntity(id=7,name="عميل",createdAt=0))
        db.transactionDao().insertTransaction(TransactionEntity.fromDomain(Transaction(id=8,customerId=7,amount=75.0,originalAmount=100.0,isPaid=false,date=0,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED)))
    }
    @Test fun encryptedExportImportRoundTripPreservesAmountsAndReturnState()=runTest {
        seed();val expected=db.transactionDao().getAllOnce();val manager=manager()
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val parsed=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            db.transactionDao().deleteAll();manager.restorePayload(parsed)
            assertEquals(expected,db.transactionDao().getAllOnce())
            assertEquals(75.0,db.transactionDao().getUnpaidAmountByCustomer(7),0.0)
        } finally { file.delete() }
    }
    @Test fun failedRestoreRollsBackExistingMerchantData()=runTest {
        seed();val before=db.transactionDao().getAllOnce()
        val corrupt=BackupPayload(customers=listOf(CustomerEntity(id=1,name="other",createdAt=0)),transactions=listOf(TransactionEntity(id=99,customerId=999,amount=1.0,isPaid=false)))
        try { manager().restorePayload(corrupt);fail("Orphan restore accepted") } catch(expected:android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(before,db.transactionDao().getAllOnce())
        assertNotNull(db.customerDao().getCustomerById(7))
    }
    @Test fun malformedFilesAreRejectedWithoutTouchingDatabase()=runTest {
        seed();val before=db.transactionDao().getAllOnce()
        for(bytes in listOf(byteArrayOf(),"not backup".toByteArray(),byteArrayOf(0x50,0x4b))) {
            try { manager().parseBackupFromBytes(bytes);fail("Corruption accepted") } catch(expected:BackupException) { }
        }
        assertEquals(before,db.transactionDao().getAllOnce())
    }
    @Test fun legacyBackupRestoresReturnStateFromItsDurableLedger()=runTest {
        val payload=BackupPayload(customers=listOf(CustomerEntity(id=7,name="fixture",createdAt=0)),transactions=listOf(TransactionEntity(id=8,customerId=7,amount=75.0,isPaid=false)),
            invoiceItems=listOf(InvoiceItem(id="line",transactionId=8,unitId="u",quantity=4.0,pricePerUnit=25.0,totalPrice=100.0).toEntity()),
            returnInvoices=listOf(ReturnInvoice(id="r",originalTransactionId=8,totalRefund=25.0).toEntity()),
            returnItems=listOf(ReturnItem(id="item",returnInvoiceId="r",unitId="u",originalQuantity=4.0,returnedQuantity=1.0,pricePerUnit=25.0).toEntity()))
        val out=ByteArrayOutputStream()
        ZipOutputStream(out).use { it.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY));it.write(Gson().toJson(payload).toByteArray());it.closeEntry() }
        val restored=manager().parseBackupFromBytes(out.toByteArray()).transactions.single().toDomain()
        assertEquals(100.0,restored.originalAmount,0.0)
        assertTrue(restored.isPartiallyReturned)
    }
    @Test fun exportReadsTablesInsideOneDatabaseSnapshot()=runTest {
        val spy=spyk(db);val customer=mockk<CustomerDao>()
        every { spy.customerDao() } returns customer
        coEvery { customer.getAllOnce() } coAnswers {
            assertTrue("Export reads must share a transaction snapshot",spy.inTransaction())
            listOf(CustomerEntity(id=7,name="fixture",createdAt=0))
        }
        val file=manager(spy).exportEncryptedBackup("fixture".toCharArray());file.delete()
    }
}
