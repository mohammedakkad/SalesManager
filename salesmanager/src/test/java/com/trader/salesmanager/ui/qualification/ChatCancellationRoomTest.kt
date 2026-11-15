package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import com.trader.salesmanager.ui.chat.ChatViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual pending-message SQLite rows and real ViewModel child jobs; remote operations never connect. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ChatCancellationRoomTest {
    private lateinit var db:AppDatabase
    private lateinit var vm:ChatViewModel
    private val repo=mockk<ChatRepository>(relaxed=true)
    private val activation=mockk<ActivationRepository>(relaxed=true)
    private val messages=MutableStateFlow<List<ChatMessage>>(emptyList())
    @Before fun setup()=runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java).allowMainThreadQueries().build()
        coEvery { activation.getMerchantCode() } returns "tenant"
        every { repo.getMessages("tenant") } returns messages
        vm=ChatViewModel(repo,activation,db.pendingMessageDao())
        withTimeout(10000) { vm.uiState.first { !it.isLoading && it.merchantId=="tenant" } }
        Unit
    }
    @After fun cleanup()=runBlocking {
        vm.viewModelScope.coroutineContext[Job]!!.cancelAndJoin(); db.close(); Dispatchers.resetMain()
    }
    private suspend fun joined(job:CompletableDeferred<Job>):Job = withTimeout(10000) { job.await().also { it.join() } }
    @Test fun cancelledNewSendLeavesThePersistedMessagePendingRatherThanFailed()=runBlocking {
        val job=CompletableDeferred<Job>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers {
            job.complete(currentCoroutineContext()[Job]!!); throw CancellationException("cancelled upload")
        }
        vm.updateInput("  durable question  "); vm.sendOrSaveMessage(); val child=joined(job)
        val rows=db.pendingMessageDao().getAll("tenant").first()
        assertEquals(1,rows.size); assertEquals("durable question",rows.single().text)
        assertFalse("Cancellation must not become a durable transport failure",rows.single().isFailed)
        assertTrue("Cancellation must propagate to the sending child",child.isCancelled)
    }
    @Test fun cancelledRetryRetainsStableIdentityWithoutInventingASecondFailure()=runBlocking {
        val original=PendingMessageEntity("stable","tenant","queued question","store",isFailed=true)
        db.pendingMessageDao().insert(original)
        withTimeout(10000) { vm.uiState.first { it.pendingMessages.any { p->p.tempId=="stable" } } }
        val job=CompletableDeferred<Job>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers {
            assertEquals("stable",secondArg<ChatMessage>().id)
            job.complete(currentCoroutineContext()[Job]!!); throw CancellationException("cancelled retry")
        }
        vm.retryMessage("stable"); val child=joined(job)
        assertEquals(original.copy(isFailed=false),db.pendingMessageDao().getAll("tenant").first().single())
        assertTrue(child.isCancelled)
    }
    @Test fun ordinarySendFailureStillPersistsFailedStatusForExplicitRetry()=runBlocking {
        val job=CompletableDeferred<Job>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers {
            job.complete(currentCoroutineContext()[Job]!!); throw java.io.IOException("offline")
        }
        vm.updateInput("question");vm.sendOrSaveMessage();val child=joined(job)
        assertTrue(db.pendingMessageDao().getAll("tenant").first().single().isFailed)
        assertFalse(child.isCancelled)
    }
    @Test fun successfulRetryDeletesExactlyTheOriginalPendingIdentity()=runBlocking {
        val original=PendingMessageEntity("stable","tenant","question","store",isFailed=true)
        db.pendingMessageDao().insert(original)
        withTimeout(10000) { vm.uiState.first { it.pendingMessages.isNotEmpty() } }
        val job=CompletableDeferred<Job>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers {
            assertEquals("tenant",firstArg<String>());assertEquals("stable",secondArg<ChatMessage>().id)
            job.complete(currentCoroutineContext()[Job]!!)
        }
        vm.retryMessage("stable");joined(job)
        assertTrue(db.pendingMessageDao().getAll("tenant").first().isEmpty())
    }
    private suspend fun selectOwnMessages() {
        messages.value=listOf(ChatMessage("a","first","tenant"),ChatMessage("b","second","tenant"))
        withTimeout(10000) { vm.uiState.first { it.messages.size==2 } }
        vm.onLongPress(vm.uiState.value.messages[0]);vm.onTap(vm.uiState.value.messages[1])
    }
    @Test fun cancelledBatchDeletionStopsBeforeDispatchingAnotherMessage()=runBlocking {
        selectOwnMessages(); vm.requestDeleteSelected();val job=CompletableDeferred<Job>();val calls=mutableListOf<String>()
        coEvery { repo.deleteMessage(any(),any()) } coAnswers {
            calls+=secondArg<String>();job.complete(currentCoroutineContext()[Job]!!)
            throw CancellationException("cancelled deletion")
        }
        vm.confirmDeleteForEveryone(); val child=joined(job)
        assertEquals("No later delete may be dispatched after cancellation",1,calls.size)
        assertTrue(child.isCancelled);assertEquals(setOf("a","b"),vm.uiState.value.locallyDeletedIds)
    }
    @Test fun cancelledEditPropagatesInsteadOfReportingACompletedChild()=runBlocking {
        messages.value=listOf(ChatMessage("a","old","tenant"))
        withTimeout(10000) { vm.uiState.first { it.messages.size==1 } }
        vm.onLongPress(vm.uiState.value.messages.single());vm.startEditSelected();vm.updateInput("new")
        val job=CompletableDeferred<Job>()
        coEvery { repo.editMessage(any(),any(),any()) } coAnswers {
            job.complete(currentCoroutineContext()[Job]!!);throw CancellationException("cancelled edit")
        }
        vm.sendOrSaveMessage();assertTrue(joined(job).isCancelled)
    }
    @Test fun multipleSelectionCannotEnterSingleMessageEditor()=runBlocking {
        selectOwnMessages();assertFalse(vm.uiState.value.canEdit)
        vm.startEditSelected();assertNull(vm.uiState.value.editingMessage)
        assertEquals(setOf("a","b"),vm.uiState.value.selectedIds)
    }
    @Test fun remotelyDeletedSelectionCannotEnterEditorFromAStaleUiCallback()=runBlocking {
        val own=ChatMessage("a","old","tenant");messages.value=listOf(own)
        withTimeout(10000) { vm.uiState.first { it.messages.size==1 } };vm.onLongPress(own)
        messages.value=listOf(own.copy(deletedAt=com.google.firebase.Timestamp(1,0)))
        withTimeout(10000) { vm.uiState.first { it.messages.single().isDeleted } }
        assertFalse(vm.uiState.value.canEdit);vm.startEditSelected();assertNull(vm.uiState.value.editingMessage)
    }
}
