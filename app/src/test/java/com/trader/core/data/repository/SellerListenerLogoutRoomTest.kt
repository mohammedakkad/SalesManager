package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SellerListenerLogoutRoomTest {
    private lateinit var db:AppDatabase
    private val actor=MutableStateFlow("m1")
    private val activation=mockk<ActivationRepository>()
    private val sync=mockk<FirebaseSyncService>()
    private val jobs=CopyOnWriteArrayList<Job>()
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        every{activation.observeMerchantCode()} returns actor.onStart{jobs+=currentCoroutineContext().job}
        coEvery{activation.getMerchantCode()} answers {actor.value}
    }
    @After fun cleanup()=runBlocking {jobs.toList().forEach{it.cancelAndJoin()};db.close()}
    private fun <T> tracked(events:MutableSharedFlow<T>)=flow {
        jobs+=currentCoroutineContext().job;events.collect{emit(it)}
    }
    @Test fun paymentMethodListenerUnregistersOnLogoutRatherThanKeepingTheOldMerchant()=runBlocking {
        val events=MutableSharedFlow<List<PaymentMethod>>()
        every{sync.observePaymentMethods(any())} returns tracked(events)
        PaymentMethodRepositoryImpl(db.paymentMethodDao(),sync,activation,db.cashBoxDao(),db.cashBoxMovementDao())
        withTimeout(5000){events.subscriptionCount.first{it==1}}
        actor.value=""
        assertNotNull("Logout must cancel the old payment listener",withTimeoutOrNull(1000){events.subscriptionCount.first{it==0}})
    }
    @Test fun employeeListenerUnregistersOnLogoutRatherThanFilteringOutTheSignal()=runBlocking {
        val events=MutableSharedFlow<List<Employee>>()
        every{sync.observeEmployees(any())} returns tracked(events)
        EmployeeRepositoryImpl(db.employeeDao(),"m1",sync,activation)
        withTimeout(5000){events.subscriptionCount.first{it==1}}
        actor.value=""
        assertNotNull("Logout must cancel the old employee listener",withTimeoutOrNull(1000){events.subscriptionCount.first{it==0}})
    }
}
