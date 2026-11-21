package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.chat.ChatViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ChatRetryConcurrencyRoomTest {
    private lateinit var db:AppDatabase
    private val repo=mockk<ChatRepository>(relaxed=true)
    private val activation=mockk<ActivationRepository>(relaxed=true)
    private val store=ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).allowMainThreadQueries()
            .setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.build()
        coEvery { activation.getMerchantCode() } returns "tenant";every { repo.getMessages("tenant") } returns MutableStateFlow(emptyList())
    }
    @After fun cleanup() { store.clear();db.close();Dispatchers.resetMain() }
    private suspend fun TestScope.model():ChatViewModel {
        db.pendingMessageDao().insert(PendingMessageEntity("stable","tenant","queued","store",isFailed=true))
        val vm=ChatViewModel(repo,activation,db.pendingMessageDao());store.put("chat",vm);advanceUntilIdle()
        assertEquals("stable",vm.uiState.value.pendingMessages.single().tempId);return vm
    }
    @Test fun repeatedRetryWhileSendingUsesOneRemoteAttemptAndOnePersistedIdentity()=runTest {
        val vm=model();val release=CompletableDeferred<Unit>();var calls=0
        coEvery { repo.sendMessage(any(),any()) } coAnswers { calls++;assertEquals("stable",secondArg<ChatMessage>().id);release.await() }
        try {
            vm.retryMessage("stable");advanceUntilIdle();assertEquals(1,calls)
            vm.retryMessage("stable");advanceUntilIdle();assertEquals("An active identity must not start another attempt",1,calls)
            assertEquals(1,db.pendingMessageDao().getAll("tenant").first().size)
        } finally { release.complete(Unit);advanceUntilIdle() }
        assertTrue(db.pendingMessageDao().getAll("tenant").first().isEmpty())
    }
    @Test fun staleDismissCallbackCannotDeleteAnIdentityStillBeingSent()=runTest {
        val vm=model();val release=CompletableDeferred<Unit>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers { release.await() }
        try {
            vm.retryMessage("stable");advanceUntilIdle();vm.dismissFailedMessage("stable");advanceUntilIdle()
            assertEquals("Recovery intent must survive a stale dismiss during an upload",listOf("stable"),db.pendingMessageDao().getAll("tenant").first().map { it.tempId })
        } finally { release.complete(Unit);advanceUntilIdle() }
    }
    @Test fun staleDismissCannotRemoveAnotherMerchantsPendingRowButOwnDismissStillWorks()=runTest {
        val vm=model()
        db.pendingMessageDao().insert(PendingMessageEntity("foreign","other","other queued","store",isFailed=true));advanceUntilIdle()
        vm.dismissFailedMessage("foreign");advanceUntilIdle()
        assertEquals(listOf("foreign"),db.pendingMessageDao().getAll("other").first().map { it.tempId })
        assertEquals(listOf("stable"),db.pendingMessageDao().getAll("tenant").first().map { it.tempId })
        vm.dismissFailedMessage("stable");advanceUntilIdle()
        assertTrue(db.pendingMessageDao().getAll("tenant").first().isEmpty())
        assertEquals(1,db.pendingMessageDao().getAll("other").first().size)
    }

}
