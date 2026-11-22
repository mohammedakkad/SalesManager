package com.trader.salesmanager.ui.settings.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Parses actual archives through the supported public API; never fabricates a Room schema. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupArchiveBoundaryTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private lateinit var manager:BackupManager
    @Before fun open() {
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        manager=BackupManager(context,db,"tenant")
    }
    @After fun close() {db.close()}
    private fun archive(vararg entries:Pair<String,String>)=ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip -> entries.forEach { (name,body) ->
            zip.putNextEntry(ZipEntry(name));zip.write(body.toByteArray(Charsets.UTF_8));zip.closeEntry()
        } }
    }.toByteArray()
    private fun json(id:Long)=Gson().toJson(BackupPayload(customers=listOf(CustomerEntity(id,"عميل $id"))))
    private suspend fun parsed(vararg entries:Pair<String,String>)=manager.parseBackupFromBytes(archive(*entries))
    private suspend fun corrupt(vararg entries:Pair<String,String>) {
        try {parsed(*entries);fail("Corrupt archive accepted")} catch (_:BackupException.Corrupted) {}
        assertTrue(db.customerDao().getAllOnce().isEmpty())
    }
    @Test fun nestedCanonicalEntryWinsOverEarlierLegacyJson()=runTest {
        val p=parsed("legacy.json" to json(1),"folder/$BACKUP_JSON_ENTRY" to json(2))
        assertEquals(2L,p.customers.single().id)
    }
    @Test fun archiveWithoutCanonicalNameUsesFirstLegacyJson()=runTest {
        val p=parsed("first.json" to json(1),"second.json" to json(2))
        assertEquals(1L,p.customers.single().id)
    }
    @Test fun directoriesAndNonJsonFilesAreSkipped()=runTest {
        val p=parsed("directory.json/" to "","readme.txt" to "not a backup","legacy.json" to json(7))
        assertEquals(7L,p.customers.single().id)
    }
    @Test fun damagedZipCompressionMethodIsReportedAsCorruption()=runTest {
        val damaged=archive(BACKUP_JSON_ENTRY to json(7))
        damaged[8]=99
        try {manager.parseBackupFromBytes(damaged);fail("Damaged ZIP accepted")}catch(_:BackupException.Corrupted){}
        assertTrue(db.customerDao().getAllOnce().isEmpty())
    }
    @Test fun archiveWithoutJsonIsRejected()=runTest {corrupt("readme.txt" to "not a backup")}
    @Test fun jsonNullIsRejectedInsteadOfProducingARestorePayload()=runTest {corrupt(BACKUP_JSON_ENTRY to "null")}
    @Test fun jsonArrayCannotMasqueradeAsPayloadObject()=runTest {corrupt(BACKUP_JSON_ENTRY to "[]")}
    @Test fun invalidCustomerCollectionCannotMasqueradeAsValidPayload()=runTest {corrupt(BACKUP_JSON_ENTRY to "{\"customers\":42}")}
    @Test fun encryptedArchiveRequiresPasswordBeforeParsing()=runTest {
        val encrypted=BackupCrypto.encryptZip(archive(BACKUP_JSON_ENTRY to json(7)),"fixture".toCharArray(),BACKUP_FORMAT_VERSION,1)
        assertTrue(manager.requiresPassword(encrypted))
        try {manager.parseBackupFromBytes(encrypted);fail("Password omitted")} catch(_:BackupException.PasswordRequired) {}
        assertTrue(db.customerDao().getAllOnce().isEmpty())
    }
    @Test fun explicitNullLegacyCollectionsNormalizeBeforeValidation()=runTest {
        val collections=listOf("customers","transactions","paymentMethods","products","productUnits","stockMovements","invoiceItems","inventorySessions","inventorySessionItems","returnInvoices","returnItems","employees","cashBoxes","cashBoxMovements","saleOperations","saleOutbox")
        val body=collections.joinToString(",","{\"schemaVersion\":22,","}"){"\"$it\":null"}
        val p=parsed(BACKUP_JSON_ENTRY to body)
        assertEquals(BackupPayload(exportedAt=p.exportedAt),p)
        assertThrows(BackupException.EmptyBackup::class.java){manager.validatePayload(p)}
    }
    @Test fun paymentMethodOnlyBackupIsValidAndRestoresVisitorCustomer()=runTest {
        val method=PaymentMethodEntity(1,"بطاقة",PaymentType.OTHER.name)
        val p=BackupPayload(paymentMethods=listOf(method));manager.validatePayload(p);manager.restorePayload(p)
        assertEquals(listOf(method),db.paymentMethodDao().getAllOnce())
        assertEquals(-1L,db.customerDao().getAllOnce().single().id)
    }
    @Test fun productOnlyBackupIsValidWithoutInventingSaleRecords()=runTest {
        val product=ProductEntity("product",null,"سلعة","fixture",null,"tenant",1,1,"PENDING")
        val p=BackupPayload(products=listOf(product));manager.validatePayload(p);manager.restorePayload(p)
        assertEquals(product,db.productDao().getById(product.id)!!.product)
        assertTrue(db.transactionDao().getAllOnce().isEmpty())
    }
    @Test fun ordinarySessionLookupFailureRetainsStorageFailureContract()=runTest {
        val failure=java.io.IOException("offline session read failure")
        val m=BackupManager(context,db,"tenant",currentMerchant={throw failure})
        try {m.exportEncryptedBackup("fixture".toCharArray());fail("Export should fail")}
        catch(actual:BackupException.StorageFailed) {assertSame(BackupException.StorageFailed,actual)}
        assertFalse(db.inTransaction())
    }
}
