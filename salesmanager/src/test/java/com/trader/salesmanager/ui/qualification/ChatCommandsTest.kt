package com.trader.salesmanager.ui.qualification

import androidx.lifecycle.ViewModelStore
import com.google.firebase.Timestamp
import com.trader.core.data.local.dao.PendingMessageDao
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.chat.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatCommandsTest {
    private val repo=mockk<ChatRepository>(relaxed=true);private val activation=mockk<ActivationRepository>(relaxed=true)
    private val pending=mockk<PendingMessageDao>(relaxed=true);private val store=ViewModelStore()
    private val messages=MutableStateFlow<List<ChatMessage>>(emptyList());private val queued=MutableStateFlow<List<PendingMessageEntity>>(emptyList())
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { activation.getMerchantCode() } returns "m1";every { repo.getMessages("m1") } returns messages;every { pending.getAll("m1") } returns queued
    }
    @After fun cleanup() { store.clear();Dispatchers.resetMain() }
    private fun model()=ChatViewModel(repo,activation,pending).also{store.put("chat",it)}
    @Test fun sendingPersistsBeforeDispatchAndDeletesOnlySameStableId()=runTest {
        val vm=model();runCurrent();val order=mutableListOf<String>();var saved:PendingMessageEntity?=null
        coEvery { pending.insert(any()) } coAnswers {saved=firstArg();order+="persist"}
        coEvery { repo.sendMessage(any(),any()) } coAnswers { assertEquals(saved!!.tempId,secondArg<ChatMessage>().id);order+="send" }
        coEvery { pending.delete(any()) } coAnswers {assertEquals(saved!!.tempId,firstArg());order+="ack"}
        vm.updateInput("  سؤال  ");vm.sendOrSaveMessage();runCurrent()
        assertEquals(listOf("persist","send","ack"),order);assertEquals("سؤال",saved!!.text);assertEquals("m1",saved!!.merchantId);assertEquals("",vm.uiState.value.inputText)
    }
    @Test fun failedSendRetainsPendingAndRetryUsesOriginalIdentity()=runTest {
        val vm=model();runCurrent();var record:PendingMessageEntity?=null
        coEvery { pending.insert(any()) } coAnswers {record=firstArg()};coEvery { repo.sendMessage(any(),any()) } throws java.io.IOException("offline fixture")
        vm.updateInput("سؤال");vm.sendOrSaveMessage();runCurrent()
        coVerify { pending.setFailed(record!!.tempId,true) };coVerify(exactly=0) {pending.delete(any())}
        queued.value=listOf(record!!.copy(isFailed=true));runCurrent();coEvery { repo.sendMessage(any(),any()) } just Runs
        vm.retryMessage(record!!.tempId);runCurrent();coVerify {repo.sendMessage("m1",match{it.id==record!!.tempId})};coVerify {pending.delete(record!!.tempId)}
    }
    @Test fun loadMarksOnlyUnreadAdminMessagesAndHidesPendingEcho()=runTest {
        messages.value=listOf(ChatMessage("read","مقروء","admin",isRead=true),ChatMessage("unread","جديد","admin"),ChatMessage("own","لي","m1"))
        queued.value=listOf(PendingMessageEntity("own","m1","لي","تاجر"))
        val vm=model();runCurrent();coVerify(exactly=1) {repo.markAsRead("m1","unread")};coVerify(exactly=0){repo.markAsRead("m1","own")}
        assertEquals(setOf("read","unread"),vm.uiState.value.visibleMessages.map{it.id}.toSet())
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun selectionCopiesChronologicallyAndNeverCopiesDeletedContent()=runTest {
        val a=ChatMessage("a","أول","m1",timestamp=Timestamp(1,0));val b=ChatMessage("b","ثان","admin",timestamp=Timestamp(2,0));val c=ChatMessage("c","محذوف","admin",deletedAt=Timestamp(3,0))
        messages.value=listOf(b,c,a);val vm=model();runCurrent();vm.onTap(a);assertFalse(vm.uiState.value.isSelectionMode)
        vm.onLongPress(a);vm.onTap(b);vm.onLongPress(c);assertEquals("أول\nثان",vm.buildCopyText());assertFalse(vm.uiState.value.canEdit)
        vm.toggleSelection("c");vm.toggleSelection("b");assertTrue(vm.uiState.value.canEdit);vm.cancelSelection();assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }
    @Test fun editOwnMessageTrimsChangedTextAndCannotEditOtherSender()=runTest {
        val own=ChatMessage("a","قديم","m1");val other=ChatMessage("b","آخر","admin");messages.value=listOf(own,other)
        val vm=model();runCurrent();vm.onLongPress(other);vm.startEditSelected();assertNull(vm.uiState.value.editingMessage)
        vm.cancelSelection();vm.onLongPress(own);vm.startEditSelected();assertEquals("قديم",vm.uiState.value.inputText)
        vm.updateInput("  جديد  ");vm.sendOrSaveMessage();runCurrent();coVerify {repo.editMessage("m1","a","جديد")};assertNull(vm.uiState.value.editingMessage)
        vm.onLongPress(own);vm.startEditSelected();vm.sendOrSaveMessage();runCurrent();coVerify(exactly=1){repo.editMessage(any(),any(),any())}
    }
    @Test fun deletionOfOtherSenderIsLocalAndOwnDeletionWaitsForExplicitChoice()=runTest {
        val own=ChatMessage("a","لي","m1");val other=ChatMessage("b","آخر","admin");messages.value=listOf(own,other)
        val vm=model();runCurrent();vm.onLongPress(other);vm.requestDeleteSelected();assertEquals(setOf("b"),vm.uiState.value.locallyDeletedIds)
        coVerify(exactly=0){repo.deleteMessage(any(),any())};vm.onLongPress(own);vm.requestDeleteSelected();assertTrue(vm.uiState.value.showDeleteDialog)
        vm.dismissDeleteDialog();assertTrue(vm.uiState.value.pendingOwnDeleteIds.isEmpty())
        vm.onLongPress(own);vm.requestDeleteSelected();vm.confirmDeleteForMe();assertEquals(setOf("a","b"),vm.uiState.value.locallyDeletedIds);coVerify(exactly=0){repo.deleteMessage(any(),any())}
    }
    @Test fun remoteDeleteFailureKeepsMessageHiddenAndSuccessRemovesTemporaryHide()=runTest {
        val own=ChatMessage("a","لي","m1");messages.value=listOf(own);val vm=model();runCurrent()
        coEvery {repo.deleteMessage(any(),any())} throws java.io.IOException("offline")
        vm.onLongPress(own);vm.requestDeleteSelected();vm.confirmDeleteForEveryone();runCurrent();assertTrue("a" in vm.uiState.value.locallyDeletedIds)
        coEvery {repo.deleteMessage(any(),any())} just Runs
        vm.onLongPress(own);vm.requestDeleteSelected();vm.confirmDeleteForEveryone();runCurrent();assertFalse("a" in vm.uiState.value.locallyDeletedIds)
        vm.dismissFailedMessage("pending");runCurrent();coVerify {pending.delete("pending")}
    }
}
