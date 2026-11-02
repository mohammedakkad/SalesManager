package com.trader.salesmanager.sales.outbox

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Replaces the retired best-effort delete tests with the actual durable CoroutineWorker. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SaleOutboxWorkerTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private val session=MutableStateFlow("fixture")
    private var transport:OutboxTransport=OutboxTransport { RemoteDelivery.Applied("receipt") }
    private fun open() { db=Room.databaseBuilder(context,AppDatabase::class.java,"outbox-worker.db").allowMainThreadQueries().build() }
    @Before fun setup() {
        context.deleteDatabase("outbox-worker.db");open()
        val activation=mockk<ActivationRepository>()
        coEvery { activation.getMerchantCode() } answers { session.value }
        startKoin { modules(module {
            single<AppDatabase> { db };single<ActivationRepository> { activation }
            single { OutboxDispatcher(db,OutboxTransport { transport.deliver(it) },session,{session.value}) }
        }) }
    }
    @After fun close() { stopKoin();db.close();context.deleteDatabase("outbox-worker.db") }
    private suspend fun enqueue() {
        db.saleOutboxDao().insert(SaleOutboxEntity(eventId="deletion",operationId="delete",merchantId="fixture",aggregateId=8,aggregateRevision=1,
            eventType="DELETE",payloadVersion=1,payload=Gson().toJson(SalePayload(changes=emptyList(),
                local=SaleLocalState(null,emptyList(),emptyList(),emptyList(),emptyList(),emptyList()))),createdAt=1,updatedAt=1))
    }
    private fun worker()=TestListenableWorkerBuilder<SaleOutboxWorker>(context).build()
    @Test fun workerRecoversDurableDeletionAfterDatabaseRestart()=runBlocking {
        enqueue();db.close();open()
        assertEquals(Result.success(),worker().doWork())
        assertEquals("ACKNOWLEDGED",db.saleOutboxDao().all().single().state)
    }
    @Test fun workerDoesNotDispatchPreviousMerchantAfterSwitch()=runBlocking {
        enqueue();session.value="other";var calls=0;transport=OutboxTransport { calls++;RemoteDelivery.Applied("receipt") }
        assertEquals(Result.success(),worker().doWork());assertEquals(0,calls)
        assertEquals("PENDING",db.saleOutboxDao().all().single().state)
    }
    @Test fun workerReturnsRetryAndKeepsDeletionAfterNetworkFailure()=runBlocking {
        enqueue();transport=OutboxTransport { error("offline") }
        assertEquals(Result.retry(),worker().doWork())
        assertEquals("FAILED",db.saleOutboxDao().all().single().state)
    }
    @Test fun cancelledWorkerPreservesRetryIntent()=runBlocking {
        enqueue();val entered=CompletableDeferred<Unit>();transport=OutboxTransport { entered.complete(Unit);awaitCancellation() }
        val work=launch(Dispatchers.Default) { worker().doWork() }
        withTimeout(10000) { entered.await() };work.cancelAndJoin()
        assertEquals("FAILED",db.saleOutboxDao().all().single().state)
        assertNull(db.saleOutboxDao().all().single().acknowledgedAt)
    }
}
