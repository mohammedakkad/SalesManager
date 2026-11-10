package com.trader.core.data.remote

import android.app.Application
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class SellerRealtimeRecoveryTest {
    @Test fun permissionDeniedDoesNotRetryOrInventAnEmptySnapshot()=runTest {
        var attempts=0
        val values=flow<List<String>>{attempts++;throw SellerRealtimeException(DatabaseError.PERMISSION_DENIED)}.recoverSellerRealtime().toList()
        assertEquals(1,attempts);assertTrue(values.isEmpty());assertEquals(0,currentTime)
    }
    @Test fun expiredCredentialsDoNotEnterANetworkRetryLoop()=runTest {
        var attempts=0
        flow<Int>{attempts++;throw SellerRealtimeException(DatabaseError.EXPIRED_TOKEN)}.recoverSellerRealtime().collect()
        assertEquals(1,attempts);assertEquals(0,currentTime)
    }
    @Test fun transientFailuresUseTwoBoundedDelaysThenDeliverTheActualSnapshot()=runTest {
        var attempts=0
        val values=flow {attempts++;if(attempts<=2)throw SellerRealtimeException(DatabaseError.NETWORK_ERROR);emit(listOf("actual"))}.recoverSellerRealtime().toList()
        assertEquals(3,attempts);assertEquals(1500,currentTime);assertEquals(listOf(listOf("actual")),values)
    }
    @Test fun permanentDisconnectionStopsAfterThreeAttemptsWithoutDeletingLocalRows()=runTest {
        var attempts=0
        val values=flow<List<String>>{attempts++;throw SellerRealtimeException(DatabaseError.DISCONNECTED)}.recoverSellerRealtime().toList()
        assertEquals(3,attempts);assertEquals(1500,currentTime);assertTrue(values.isEmpty())
    }
    @Test fun unavailableBackendUsesTheSameFiniteRetryBudget()=runTest {
        var attempts=0
        flow<Int>{attempts++;throw SellerRealtimeException(DatabaseError.UNAVAILABLE)}.recoverSellerRealtime().collect()
        assertEquals(3,attempts);assertEquals(1500,currentTime)
    }
    @Test fun cancellationDuringBackoffCannotResubscribe()=runTest {
        var attempts=0
        val job=flow<Int>{attempts++;throw SellerRealtimeException(DatabaseError.NETWORK_ERROR)}.recoverSellerRealtime().launchIn(backgroundScope)
        runCurrent();assertEquals(1,attempts);job.cancelAndJoin();advanceTimeBy(10000);runCurrent()
        assertEquals(1,attempts);assertTrue(job.isCancelled)
    }
    @Test fun callerCancellationAndProgrammingErrorsAreNotHiddenAsSdkFailures()=runTest {
        val bug=IllegalStateException("programming fixture")
        try{flow<Int>{throw bug}.recoverSellerRealtime().collect();fail("Bug swallowed")}catch(actual:IllegalStateException){assertSame(bug,actual)}
        val cancellation=CancellationException("caller fixture")
        try{flow<Int>{throw cancellation}.recoverSellerRealtime().collect();fail("Cancellation swallowed")}catch(actual:CancellationException){assertSame(cancellation,actual)}
    }
    @Test fun switchingMerchantDuringRetryCancelsThePreviousAccount()=runTest {
        val merchant=MutableStateFlow("m1");val attempts=mutableListOf<String>();val values=mutableListOf<String>()
        val job=merchant.flatMapLatest { code -> flow {
            if(code.isBlank())return@flow
            attempts+=code
            if(code=="m1")throw SellerRealtimeException(DatabaseError.NETWORK_ERROR)
            emit(code);awaitCancellation()
        }.recoverSellerRealtime() }.onEach{values+=it}.launchIn(backgroundScope)
        runCurrent();merchant.value="m2";runCurrent();advanceTimeBy(10000);runCurrent()
        assertEquals(listOf("m1","m2"),attempts);assertEquals(listOf("m2"),values)
        merchant.value="";runCurrent();job.cancelAndJoin()
    }
}
