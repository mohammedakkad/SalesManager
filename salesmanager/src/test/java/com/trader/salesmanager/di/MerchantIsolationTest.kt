package com.trader.salesmanager.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SaleOutboxEntity
import com.trader.core.data.remote.ChatService
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.remote.ProductFirestoreService
import com.trader.core.domain.model.Employee
import com.trader.core.domain.model.MerchantStatus
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.EmployeeRepository
import com.trader.core.domain.repository.InventoryRepository
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.util.NetworkMonitor
import com.trader.salesmanager.sales.outbox.DispatchResult
import com.trader.salesmanager.sales.outbox.OutboxDispatcher
import com.trader.salesmanager.sales.outbox.OutboxTransport
import com.trader.salesmanager.sales.outbox.RemoteDelivery
import com.trader.salesmanager.sales.outbox.SaleLocalState
import com.trader.salesmanager.sales.outbox.SalePayload
import com.trader.salesmanager.ui.activation.MerchantWatcherViewModel
import com.trader.salesmanager.ui.inventory.session.InventorySessionViewModel
import com.trader.salesmanager.ui.settings.sessions.SessionsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Merchant A -> deactivate -> merchant B in ONE process and ONE Koin graph (the production
 * salesManagerModule), exactly like ActivationViewModel.deactivate() / MerchantWatcherViewModel.
 * Every merchant-scoped read/write/sync path must use the merchant that is active at the time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class MerchantIsolationTest {
    private lateinit var db: AppDatabase
    private lateinit var app: KoinApplication
    private val store = ViewModelStore()

    /** Stateful fake of the DataStore-backed activation: "" = deactivated. */
    private val code = MutableStateFlow("")
    private val status = MutableSharedFlow<MerchantStatus?>()
    private val activation = mockk<ActivationRepository>(relaxed = true).also {
        coEvery { it.getMerchantCode() } answers { code.value }
        every { it.observeMerchantCode() } returns code
        coEvery { it.isActivated() } answers { code.value.isNotBlank() }
        coEvery { it.deactivate() } answers { code.value = "" }
        coEvery { it.saveActivationStatus(any(), any()) } answers { code.value = if (firstArg()) secondArg() else "" }
        every { it.observeMerchantStatus() } returns status
    }
    private val delivered = mutableListOf<String>()
    private val firebasePaths = mutableListOf<String>()

    private fun activate(merchant: String) = runBlocking { activation.saveActivationStatus(true, merchant) }
    private fun deactivate() = runBlocking { activation.deactivate() }

    @Before fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        mockkStatic(FirebaseDatabase::class); mockkStatic(FirebaseFirestore::class)
        // Record the Firebase path segments a view model builds; no real backend is reachable.
        val reference = mockk<DatabaseReference>(relaxed = true)
        every { reference.child(any()) } answers { firebasePaths += firstArg<String>(); reference }
        val database = mockk<FirebaseDatabase>(relaxed = true)
        every { database.getReference(any()) } answers { firebasePaths += firstArg<String>(); reference }
        every { FirebaseDatabase.getInstance() } returns database
        every { FirebaseFirestore.getInstance() } throws IllegalStateException("Real Firestore forbidden in offline graph tests")
        val products = mockk<ProductRepository>(relaxed = true).also { every { it.getAllProducts() } returns flowOf(emptyList()) }
        app = koinApplication {
            androidContext(context)
            modules(salesManagerModule, module {
                single<AppDatabase> { db }
                single<ActivationRepository> { activation }
                single { mockk<FirebaseSyncService>(relaxed = true) }
                single { mockk<ProductFirestoreService>(relaxed = true) }
                single { mockk<ChatService>(relaxed = true) }
                single { mockk<NetworkMonitor>(relaxed = true) }
                single<ProductRepository> { products }
                single<OutboxTransport> { OutboxTransport { delivered += it.merchantId; RemoteDelivery.Applied("receipt") } }
                single { SavedStateHandle() }
            })
        }
    }

    @After fun cleanup() {
        store.clear(); app.close(); db.close()
        unmockkStatic(FirebaseDatabase::class); unmockkStatic(FirebaseFirestore::class); Dispatchers.resetMain()
    }

    @Test fun employeesAreWrittenAndReadUnderTheMerchantActiveAtTheTime() = runBlocking {
        activate("A")
        val repo = app.koin.get<EmployeeRepository>()
        repo.addOrUpdate(Employee(name = "a-cashier", pinCode = "1111"))
        deactivate(); activate("B")
        repo.addOrUpdate(Employee(name = "b-cashier", pinCode = "2222"))
        val rows = db.employeeDao().getAllOnce("B")
        assertEquals("B employee must be stored under B", listOf("b-cashier"), rows.map { it.name })
        assertEquals(listOf("b-cashier"), withTimeout(5000) { repo.observeEmployees().first() }.map { it.name })
        assertEquals("A's data stays with A", listOf("a-cashier"), db.employeeDao().getAllOnce("A").map { it.name })
    }

    @Test fun graphResolvedBeforeActivationStillUsesTheMerchantActivatedLater() = runBlocking {
        val repo = app.koin.get<EmployeeRepository>() // resolved while deactivated, e.g. at startup
        activate("A")
        repo.addOrUpdate(Employee(name = "a-cashier", pinCode = "1111"))
        assertEquals(listOf("a-cashier"), db.employeeDao().getAllOnce("A").map { it.name })
        assertTrue(db.employeeDao().getAllOnce("").isEmpty())
    }

    @Test fun inventorySessionsStartUnderTheNewMerchant() = runBlocking {
        activate("A")
        val repo = app.koin.get<InventoryRepository>()
        repo.startNewSession("A")
        deactivate(); activate("B")
        val vm = app.koin.get<InventorySessionViewModel>().also { store.put("inventory", it) }
        vm.startNewSession()
        val sessions = withTimeout(5000) { db.inventoryDao().getAllSessionsOnce().also { check(it.size == 2) } }
        assertEquals(listOf("A", "B"), sessions.map { it.merchantId }.sorted())
    }

    @Test fun sessionManagementTargetsTheNewMerchantFirebaseNode() {
        activate("A")
        app.koin.get<SessionsViewModel>().also { store.put("sessions-a", it) }
        deactivate(); activate("B"); firebasePaths.clear()
        val vm = app.koin.get<SessionsViewModel>().also { store.put("sessions-b", it) }
        vm.revokeSession("device-1")
        assertTrue("Firebase path must use B, was $firebasePaths", "B" in firebasePaths)
        assertTrue("Firebase path must never use A, was $firebasePaths", "A" !in firebasePaths)
    }

    @Test fun pendingOutboxOfMerchantAIsNeitherSentUnderBNorLost() = runBlocking {
        activate("A")
        db.saleOutboxDao().insert(outboxEvent("event-a", "A"))
        deactivate(); activate("B")
        db.saleOutboxDao().insert(outboxEvent("event-b", "B"))
        val dispatcher = app.koin.get<OutboxDispatcher>()
        assertEquals(DispatchResult.ACKNOWLEDGED, dispatcher.once("B"))
        assertEquals(listOf("B"), delivered)
        assertEquals("PENDING", db.saleOutboxDao().get("event-a")!!.state)
    }

    @Test fun watcherAutoDeactivationThenReactivationUsesTheNewMerchant() = runBlocking {
        activate("A")
        val repo = app.koin.get<EmployeeRepository>()
        repo.addOrUpdate(Employee(name = "a-cashier", pinCode = "1111"))
        app.koin.get<MerchantWatcherViewModel>().also { store.put("watcher", it) }
        status.emit(MerchantStatus.DISABLED)
        assertEquals("watcher deactivates a disabled merchant", "", code.value)
        activate("B")
        repo.addOrUpdate(Employee(name = "b-cashier", pinCode = "2222"))
        assertEquals(listOf("b-cashier"), db.employeeDao().getAllOnce("B").map { it.name })
    }

    private fun outboxEvent(id: String, merchant: String) = SaleOutboxEntity(
        eventId = id, operationId = id, merchantId = merchant, aggregateId = id.hashCode().toLong(), aggregateRevision = 1,
        eventType = "DELETE", payloadVersion = 1,
        payload = Gson().toJson(SalePayload(changes = emptyList(), local = SaleLocalState(null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))),
        createdAt = 1, updatedAt = 1
    )

}
