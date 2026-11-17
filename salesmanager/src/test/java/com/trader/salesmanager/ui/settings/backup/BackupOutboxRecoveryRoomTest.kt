package com.trader.salesmanager.ui.settings.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.salesmanager.sales.outbox.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recovery uses real Room transactions; the payloads represent local receipts, never remote success. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupOutboxRecoveryRoomTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private val customer=CustomerEntity(7,"fixture",createdAt=1)
    @Before fun open() {db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()}
    @After fun close() {db.close()}
    private fun manager(wakeup:()->Unit={})=BackupManager(context,db,"tenant",afterRestore=wakeup)
    private fun event(id:String="event",revision:Long=1)=SaleOutboxEntity(
        eventId=id,operationId="operation-$id",merchantId="tenant",aggregateId=8,aggregateRevision=revision,
        eventType="EDIT",payloadVersion=1,payload=Gson().toJson(SalePayload(changes=emptyList(),local=SaleLocalState(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))),createdAt=1,updatedAt=1)
    private fun payload(vararg events:SaleOutboxEntity)=BackupPayload(customers=listOf(customer),saleOutbox=events.toList())
    private suspend fun remember(e:SaleOutboxEntity):SaleOutboxEntity {db.saleOutboxDao().insert(e);return db.saleOutboxDao().get(e.eventId)!!}
    private suspend fun row(id:String="event")=db.saleOutboxDao().get(id)!!
    @Test fun knownPermanentConflictCannotBeClearedByPendingBackup()=runTest {
        val known=remember(event().copy(state="CONFLICTING",lastError="AFTER_FIRESTORE: baseline mismatch"))
        manager().restorePayload(payload(known.copy(state="PENDING",lastError=null)))
        val restored=row();assertEquals("CONFLICTING",restored.state);assertEquals(known.lastError,restored.lastError)
        assertNull(restored.leaseOwner);assertNull(restored.acknowledgedAt)
    }
    @Test fun backupConflictRemainsPermanentEvenWhenNoLocalHistoryExists()=runTest {
        manager().restorePayload(payload(event().copy(state="CONFLICTING",lastError="saved conflict")))
        assertEquals("CONFLICTING",row().state);assertEquals("saved conflict",row().lastError)
    }
    @Test fun historicalAcknowledgmentRecoversKnownReceiptWhenBackupMetadataIsMissing()=runTest {
        val old=remember(event().copy(state="ACKNOWLEDGED",acknowledgedAt=9,remoteReceipt="known receipt"))
        manager().restorePayload(payload(old.copy(state="PENDING",acknowledgedAt=null,remoteReceipt=null),event("next",2)))
        assertEquals("ACKNOWLEDGED",row().state);assertEquals(9L,row().acknowledgedAt);assertEquals("known receipt",row().remoteReceipt)
        assertEquals("PENDING",row("next").state)
    }
    @Test fun historicalAcknowledgmentKeepsBackupReceiptWhenThereIsNoKnownReceipt()=runTest {
        manager().restorePayload(payload(event().copy(state="ACKNOWLEDGED",acknowledgedAt=10,remoteReceipt="backup receipt"),event("next",2)))
        assertEquals("ACKNOWLEDGED",row().state);assertEquals(10L,row().acknowledgedAt);assertEquals("backup receipt",row().remoteReceipt)
        assertNull(row().lastError)
    }
    @Test fun latestAcknowledgmentLosesRestoredLeaseAndRequiresRemoteRevalidation()=runTest {
        manager().restorePayload(payload(event().copy(state="ACKNOWLEDGED",acknowledgedAt=10,remoteReceipt="receipt",leaseOwner="dead owner",leaseExpiresAt=999)))
        val restored=row();assertEquals("PENDING",restored.state);assertNull(restored.acknowledgedAt);assertNull(restored.remoteReceipt)
        assertNull(restored.leaseOwner);assertNull(restored.leaseExpiresAt);assertEquals("RESTORE_REVALIDATION_REQUIRED",restored.lastError)
    }
    @Test fun restoreNeverReducesKnownRetryHistoryAndRemovesSchedulingDelay()=runTest {
        val known=remember(event().copy(retryCount=8,state="FAILED",nextAttemptAt=999))
        manager().restorePayload(payload(known.copy(retryCount=2)))
        assertEquals(8,row().retryCount);assertEquals(0L,row().nextAttemptAt);assertEquals("PENDING",row().state)
    }
    @Test fun higherBackupRetryHistorySurvivesRestore()=runTest {
        val known=remember(event().copy(retryCount=2))
        manager().restorePayload(payload(known.copy(retryCount=8)))
        assertEquals(8,row().retryCount)
    }
    @Test fun foreignCommandReceiptsRollBackEarlierCustomerReplacement()=runTest {
        db.customerDao().insertAll(listOf(customer))
        val p=BackupPayload(customers=listOf(CustomerEntity(9,"replacement")),saleOperations=listOf(SaleOperationEntity("other","operation","EDIT","digest",8)))
        try {manager().restorePayload(p);fail("Foreign receipt accepted")}catch(_:IllegalArgumentException){}
        assertEquals(listOf(customer),db.customerDao().getAllOnce());assertTrue(db.saleOperationDao().allForMerchant("other").isEmpty())
    }
    @Test fun foreignOutboxRollsBackReplacementAndEarlierCommandReceiptInsert()=runTest {
        db.customerDao().insertAll(listOf(customer))
        val p=BackupPayload(customers=listOf(CustomerEntity(9,"replacement")),saleOperations=listOf(SaleOperationEntity("tenant","operation","EDIT","digest",8)),saleOutbox=listOf(event().copy(merchantId="other")))
        try {manager().restorePayload(p);fail("Foreign event accepted")}catch(_:IllegalArgumentException){}
        assertEquals(listOf(customer),db.customerDao().getAllOnce());assertTrue(db.saleOperationDao().allForMerchant("tenant").isEmpty());assertTrue(db.saleOutboxDao().all().isEmpty())
    }
    @Test fun endedSessionCannotStartDestructiveRestore()=runTest {
        db.customerDao().insertAll(listOf(customer))
        val m=BackupManager(context,db,"tenant",currentMerchant={" "})
        try {m.restorePayload(BackupPayload(customers=listOf(CustomerEntity(9,"replacement"))));fail("Ended session restored")}catch(_:IllegalArgumentException){}
        assertEquals(listOf(customer),db.customerDao().getAllOnce())
    }
    @Test fun cancelledSessionLookupLeavesExistingDatabaseUntouched()=runTest {
        db.customerDao().insertAll(listOf(customer));val cancelled=CancellationException("session read cancelled")
        val m=BackupManager(context,db,"tenant",currentMerchant={throw cancelled})
        try {m.restorePayload(BackupPayload(customers=listOf(CustomerEntity(9,"replacement"))));fail("Cancellation lost")}
        catch(actual:CancellationException){assertEquals(cancelled.message,actual.message)}
        assertEquals(listOf(customer),db.customerDao().getAllOnce());assertFalse(db.inTransaction())
    }
    @Test fun wakeupFailureDoesNotRollBackAlreadyCommittedRestore()=runTest {
        manager {throw java.io.IOException("scheduler unavailable")}.restorePayload(payload())
        assertEquals(listOf(customer),db.customerDao().getAllOnce());assertFalse(db.inTransaction())
    }
    @Test fun wakeupCancellationPropagatesAfterTheRestoreHasCommitted()=runTest {
        val cancelled=CancellationException("wakeup cancelled")
        try {manager {throw cancelled}.restorePayload(payload());fail("Cancellation lost")}
        catch(actual:CancellationException){assertEquals(cancelled.message,actual.message)}
        assertEquals(listOf(customer),db.customerDao().getAllOnce());assertFalse(db.inTransaction())
    }
}
