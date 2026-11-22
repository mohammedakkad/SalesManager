package com.trader.core.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.InvoiceItemEntity
import com.trader.core.data.local.entity.SaleOutboxEntity
import com.trader.core.data.remote.*
import com.trader.core.data.repository.InvoiceItemRepositoryImpl
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real SQLite rows, deterministic transports. No Firebase instance or remote data is used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class InvoicePendingSyncBoundaryTest {
    private lateinit var db:AppDatabase
    private lateinit var repo:InvoiceItemRepositoryImpl
    private val firestore=mockk<ProductFirestoreService>(relaxed=true)
    private val realtime=mockk<FirebaseSyncService>(relaxed=true)
    private val activation=mockk<ActivationRepository>()
    private var merchant="owner"
    private val row=InvoiceItemEntity("line",8,"product","صنف","unit","حبة",1.0,25.0,25.0,"owner","PENDING")
    @Before fun setup()=runBlocking {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        coEvery{activation.getMerchantCode()} answers{merchant}
        every{activation.observeMerchantCode()} returns emptyFlow()
        repo=InvoiceItemRepositoryImpl(db.invoiceItemDao(),firestore,realtime,"owner",db,activation)
        db.invoiceItemDao().insertInvoiceItem(row)
    }
    @After fun close(){db.close()}
    private suspend fun stored()=db.invoiceItemDao().getById("line")!!
    @Test fun bothAcknowledgementsAreRequiredToMarkTheExactInvoiceSynced()=runBlocking {
        repo.syncPendingItems()
        assertEquals(row.copy(syncStatus="SYNCED"),stored())
        coVerify(exactly=1){firestore.uploadInvoiceItems("owner",listOf(row.toDomain()))}
        coVerify(exactly=1){realtime.pushInvoiceItems("owner",listOf(row.toDomain()))}
    }
    @Test fun firestoreFailureStillAttemptsRealtimeAndLeavesARetryableFailure()=runBlocking {
        coEvery{firestore.uploadInvoiceItems(any(),any())} throws java.io.IOException("fixture firestore")
        repo.syncPendingItems();assertEquals(row.copy(syncStatus="FAILED"),stored())
        coVerify(exactly=1){realtime.pushInvoiceItems("owner",any())}
    }
    @Test fun realtimeFailureDoesNotAcknowledgeTheSuccessfulFirestoreWrite()=runBlocking {
        coEvery{realtime.pushInvoiceItems(any(),any())} throws java.io.IOException("fixture realtime")
        repo.syncPendingItems();assertEquals(row.copy(syncStatus="FAILED"),stored())
        coVerify(exactly=1){firestore.uploadInvoiceItems("owner",any())}
    }
    @Test fun bothFailuresKeepEveryOriginalFinancialField()=runBlocking {
        coEvery{firestore.uploadInvoiceItems(any(),any())} throws java.io.IOException("fixture firestore")
        coEvery{realtime.pushInvoiceItems(any(),any())} throws java.io.IOException("fixture realtime")
        repo.syncPendingItems();assertEquals(row.copy(syncStatus="FAILED"),stored())
    }
    @Test fun alreadySyncedRowsDoNotGenerateAnotherUpload()=runBlocking {
        db.invoiceItemDao().markSynced("line");repo.syncPendingItems()
        coVerify(exactly=0){firestore.uploadInvoiceItems(any(),any())};coVerify(exactly=0){realtime.pushInvoiceItems(any(),any())}
    }
    @Test fun managedAndForeignRowsAreExcludedFromTheSamePendingBatch()=runBlocking {
        val managed=row.copy(id="managed",transactionId=9)
        val foreign=row.copy(id="foreign",merchantId="other")
        db.invoiceItemDao().insertAll(listOf(managed,foreign))
        db.saleOutboxDao().insert(SaleOutboxEntity(eventId="event",operationId="operation",merchantId="owner",aggregateId=9,aggregateRevision=1,eventType="CREATE",payloadVersion=1,payload="{}",createdAt=0,updatedAt=0))
        repo.syncPendingItems()
        coVerify(exactly=1){firestore.uploadInvoiceItems("owner",listOf(row.toDomain()))}
        assertEquals(managed,db.invoiceItemDao().getById("managed"));assertEquals(foreign,db.invoiceItemDao().getById("foreign"))
    }
    @Test fun failedRowCanRetryAndOnlyThenBecomeSynced()=runBlocking {
        db.invoiceItemDao().markFailed("line");repo.syncPendingItems()
        assertEquals("SYNCED",stored().syncStatus)
        coVerify(exactly=1){realtime.pushInvoiceItems("owner",match{it.single().syncStatus==com.trader.core.domain.model.SyncStatus.FAILED})}
    }
    @Test fun firestoreCancellationPropagatesWithoutSecondTransportOrStatusMutation()=runBlocking {
        val cancelled=CancellationException("fixture cancelled firestore")
        coEvery{firestore.uploadInvoiceItems(any(),any())} throws cancelled
        try{repo.syncPendingItems();fail("Cancellation must escape")}catch(actual:CancellationException){assertSame(cancelled,actual)}
        assertEquals(row,stored());coVerify(exactly=0){realtime.pushInvoiceItems(any(),any())}
    }
    @Test fun realtimeCancellationPropagatesWithoutMarkingTheInvoiceFailed()=runBlocking {
        val cancelled=CancellationException("fixture cancelled realtime")
        coEvery{realtime.pushInvoiceItems(any(),any())} throws cancelled
        try{repo.syncPendingItems();fail("Cancellation must escape")}catch(actual:CancellationException){assertSame(cancelled,actual)}
        assertEquals(row,stored())
    }
    @Test fun accountSwitchAfterFirstTransportStopsTheSecondTransportAndLocalAcknowledgement()=runBlocking {
        coEvery{firestore.uploadInvoiceItems(any(),any())} answers{merchant="other";Unit}
        repo.syncPendingItems()
        coVerify(exactly=0){realtime.pushInvoiceItems(any(),any())};assertEquals(row,stored())
    }
    @Test fun accountSwitchDuringSecondTransportDoesNotAcknowledgeTheOldAccountsRows()=runBlocking {
        coEvery{realtime.pushInvoiceItems(any(),any())} answers{merchant="other";Unit}
        repo.syncPendingItems();assertEquals(row,stored())
    }
    @Test fun cancellationDuringANonSuspendingFirestoreCompletionStopsRealtime()=runBlocking {
        coEvery{firestore.uploadInvoiceItems(any(),any())} coAnswers{currentCoroutineContext().cancel();Unit}
        val upload=async{repo.syncPendingItems()}
        try{upload.await();fail("The child must be cancelled")}catch(_:CancellationException){}
        coVerify(exactly=0){realtime.pushInvoiceItems(any(),any())};assertEquals(row,stored())
    }
    @Test fun cancellationDuringANonSuspendingRealtimeCompletionPreventsAcknowledgement()=runBlocking {
        coEvery{realtime.pushInvoiceItems(any(),any())} coAnswers{currentCoroutineContext().cancel();Unit}
        val upload=async{repo.syncPendingItems()}
        try{upload.await();fail("The child must be cancelled")}catch(_:CancellationException){}
        assertEquals(row,stored())
    }

    @Test fun successfulOldSnapshotDoesNotAcknowledgeAnInvoiceEditedDuringUpload()=runBlocking {
        val edited=row.copy(quantity=2.0,totalPrice=50.0)
        coEvery{realtime.pushInvoiceItems(any(),any())} coAnswers{db.invoiceItemDao().insertInvoiceItem(edited);Unit}
        repo.syncPendingItems();assertEquals(edited,stored())
    }
    @Test fun failedOldSnapshotDoesNotMarkANewerInvoiceEditFailed()=runBlocking {
        val edited=row.copy(pricePerUnit=30.0,totalPrice=30.0)
        coEvery{realtime.pushInvoiceItems(any(),any())} coAnswers{db.invoiceItemDao().insertInvoiceItem(edited);throw java.io.IOException("fixture old upload")}
        repo.syncPendingItems();assertEquals(edited,stored())
    }
    @Test fun outboxTakingOwnershipDuringUploadPreventsTheLegacyAcknowledgement()=runBlocking {
        coEvery{realtime.pushInvoiceItems(any(),any())} coAnswers {
            db.saleOutboxDao().insert(SaleOutboxEntity(eventId="late-event",operationId="late-operation",merchantId="owner",aggregateId=8,aggregateRevision=1,eventType="EDIT",payloadVersion=1,payload="{}",createdAt=0,updatedAt=0));Unit
        }
        repo.syncPendingItems();assertEquals(row,stored());assertEquals("PENDING",db.saleOutboxDao().get("late-event")!!.state)
    }
    @Test fun deletingTheInvoiceWhileItUploadsDoesNotResurrectTheRow()=runBlocking {
        coEvery{realtime.pushInvoiceItems(any(),any())} coAnswers{db.invoiceItemDao().deleteForTransaction(8);Unit}
        repo.syncPendingItems();assertNull(db.invoiceItemDao().getById("line"))
    }

}
