package com.trader.salesmanager.ui.coverage

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.*
import com.google.firebase.Timestamp
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.salesmanager.ui.chat.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatSelectionBoundaryUiTest:OfflineSellerUiTest() {
    private val vm=mockk<ChatViewModel>(relaxed=true)
    private val state=MutableStateFlow(ChatUiState(merchantId="tenant",isLoading=false))
    @Before fun model() { every { vm.uiState } returns state }
    private fun render(back:()->Unit={}) { show { ChatScreen(back,vm) } }
    private fun selected(vararg msgs:ChatMessage) {
        state.value=state.value.copy(messages=msgs.toList(),selectedIds=msgs.map{it.id}.toSet(),isSelectionMode=true)
    }
    @Test fun normalBackNavigatesAndSelectionBackCancelsSelection() {
        var navigations=0;render { navigations++ }
        ui.onAllNodes(hasClickAction() and !hasSetTextAction())[0].performClick();assertEquals(1,navigations)
        ui.runOnIdle { selected(ChatMessage("a","selected","tenant")) };ui.waitForIdle()
        ui.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() };ui.waitForIdle()
        verify(exactly=1) { vm.cancelSelection() };assertEquals(1,navigations)
    }
    @Test fun ownSingleSelectionOffersEditAndDelegatesTheSelectedAction() {
        selected(ChatMessage("a","own","tenant"));render()
        ui.onNodeWithText("محددة").assertExists()
        ui.onAllNodes(hasClickAction() and !hasSetTextAction()).assertCountEquals(5)
        ui.onAllNodes(hasClickAction() and !hasSetTextAction())[2].performClick()
        verify(exactly=1) { vm.startEditSelected() };verify(exactly=0) { vm.requestDeleteSelected() }
    }
    @Test fun foreignMultiSelectionCopiesThroughTheActualClipboardAndClosesSelection() {
        selected(ChatMessage("a","first","admin"),ChatMessage("b","second","admin"))
        every { vm.buildCopyText() } returns "first\nsecond";render()
        ui.onNodeWithText("محددات").assertExists();ui.onAllNodes(hasClickAction() and !hasSetTextAction()).assertCountEquals(4)
        ui.onAllNodes(hasClickAction() and !hasSetTextAction())[1].performClick()
        val clipboard=activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("first\nsecond",clipboard.primaryClip!!.getItemAt(0).text.toString())
        verify(exactly=1) { vm.cancelSelection() };verify(exactly=0) { vm.startEditSelected() }
    }
    @Test fun deletedSelectionExposesNeitherCopyNorEditAndHidesOriginalText() {
        selected(ChatMessage("a","deleted secret","tenant",deletedAt=Timestamp(1,0)));render()
        ui.onNodeWithText("deleted secret").assertDoesNotExist()
        ui.onNodeWithText("تم حذف هذه الرسالة").assertExists()
        ui.onAllNodes(hasClickAction() and !hasSetTextAction()).assertCountEquals(3)
        ui.onAllNodes(hasClickAction() and !hasSetTextAction())[1].performClick();verify(exactly=1) { vm.requestDeleteSelected() }
        verify(exactly=0) { vm.startEditSelected() };verify(exactly=0) { vm.buildCopyText() }
    }
    @Test fun mixedSelectionBubblesDelegateTheirOwnIdentitiesOnTap() {
        selected(ChatMessage("a","own selected","tenant"),ChatMessage("b","admin selected","admin"));render()
        ui.onNodeWithText("own selected").performTouchInput { click() }
        ui.onNodeWithText("admin selected").performTouchInput { click() }
        verify(exactly=1) { vm.onTap(match { it.id=="a" }) }
        verify(exactly=1) { vm.onTap(match { it.id=="b" }) }
    }
    @Test fun editCancelClearsTheBannerWhenTheModelLeavesEditingMode() {
        val own=ChatMessage("a","old","tenant")
        state.value=state.value.copy(editingMessage=own,inputText="draft");render()
        // Normal toolbar back, edit cancel, then send; exclude the editable field click action.
        ui.onAllNodes(hasClickAction() and !hasSetTextAction())[1].performClick()
        verify(exactly=1) { vm.cancelEdit() }
        ui.runOnIdle { state.value=state.value.copy(editingMessage=null,inputText="") };ui.waitForIdle()
        ui.onNodeWithText("تعديل الرسالة").assertDoesNotExist();ui.onNodeWithText("draft").assertDoesNotExist()
    }
    @Test fun failedControlsAppearOnlyAfterPendingMessageFailure() {
        val pending=PendingMessageEntity("stable","tenant","pending body","store")
        state.value=state.value.copy(pendingMessages=listOf(pending),sendingIds=setOf("stable"));render()
        ui.onNodeWithText("pending body").assertExists();ui.onNodeWithText("إعادة").assertDoesNotExist()
        ui.onNodeWithText("فشل الإرسال").assertDoesNotExist()
        ui.runOnIdle { state.value=state.value.copy(pendingMessages=listOf(pending.copy(isFailed=true)),sendingIds=emptySet()) };ui.waitForIdle()
        ui.onNodeWithText("إعادة").performClick();verify(exactly=1) { vm.retryMessage("stable") }
    }
    @Test fun locallyDeletedAndQueuedServerEchoesDoNotRevealDuplicateContent() {
        state.value=state.value.copy(messages=listOf(ChatMessage("hidden","local secret","admin"),ChatMessage("queued","server echo","tenant")),locallyDeletedIds=setOf("hidden"),pendingMessages=listOf(PendingMessageEntity("queued","tenant","pending body","store")))
        render();ui.onNodeWithText("local secret").assertDoesNotExist();ui.onNodeWithText("server echo").assertDoesNotExist()
        ui.onNodeWithText("pending body").assertExists();ui.onNodeWithText("ابدأ محادثة مع الإدارة").assertDoesNotExist()
    }
    @Test fun emptyPromptWaitsUntilHistoryLoadingFinishes() {
        state.value=state.value.copy(isLoading=true);render();ui.onNodeWithText("ابدأ محادثة مع الإدارة").assertDoesNotExist()
        ui.runOnIdle { state.value=state.value.copy(isLoading=false) };ui.waitForIdle()
        ui.onNodeWithText("ابدأ محادثة مع الإدارة").assertExists()
    }
}
