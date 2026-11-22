package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.google.firebase.Timestamp
import com.trader.core.domain.model.*
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.salesmanager.ui.chat.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatJourneysUiTest:OfflineSellerUiTest() {
    private val vm=mockk<ChatViewModel>(relaxed=true)
    private val state=MutableStateFlow(ChatUiState(merchantId="fixture",isLoading=false))
    @Before fun fixture() { every { vm.uiState } returns state }
    @Test fun inputAndSendAreDelegatedWithoutNetwork() {
        show { ChatScreen({},vm) }
        ui.onNodeWithText("ابدأ محادثة مع الإدارة").assertExists()
        ui.onNode(hasSetTextAction()).performTextInput("مساعدة")
        verify { vm.updateInput("مساعدة") }
        // Send FAB is the final click action in the bottom input bar.
        ui.onAllNodes(hasClickAction()).onLast().performClick();verify { vm.sendOrSaveMessage() }
    }
    @Test fun deliveredReadEditedAndDeletedMessagesPreserveVisibilityRules() {
        state.value=state.value.copy(messages=listOf(
            ChatMessage("a","مرحباً","admin",timestamp=Timestamp(1700000000,0)),
            ChatMessage("b","تم الرد","fixture",timestamp=Timestamp(1700000100,0),isRead=true,editedAt=Timestamp(1700000200,0)),
            ChatMessage("c","نص محذوف سري","admin",timestamp=Timestamp(1700000300,0),deletedAt=Timestamp(1700000400,0))))
        show { ChatScreen({},vm) }
        ui.onNodeWithText("مرحباً").assertExists();ui.onNodeWithText("تم الرد").assertExists()
        ui.onNodeWithText("تم حذف هذه الرسالة").assertExists();ui.onNodeWithText("نص محذوف سري").assertDoesNotExist()
        ui.onNodeWithText("معدّل").assertExists()
        ui.onNodeWithText("تم الرد").performTouchInput { longClick() };verify { vm.onLongPress(match { it.id=="b" }) }
    }
    @Test fun failedPendingMessageProvidesRetryAndDismissWithStableId() {
        state.value=state.value.copy(pendingMessages=listOf(PendingMessageEntity("stable","fixture","رسالة مؤجلة","متجر",isFailed=true)))
        show { ChatScreen({},vm) }
        ui.onNodeWithText("فشل الإرسال").assertExists()
        ui.onNodeWithText("إعادة").performClick();verify { vm.retryMessage("stable") }
        ui.onNodeWithText("حذف").performClick();verify { vm.dismissFailedMessage("stable") }
    }
    @Test fun ownMessageDeletionOffersDistinctLocalAndEveryoneActions() {
        state.value=state.value.copy(messages=listOf(ChatMessage("a","رسالتي","fixture")),selectedIds=setOf("a"),isSelectionMode=true,showDeleteDialog=true,pendingOwnDeleteIds=setOf("a"),editingMessage=ChatMessage("a","رسالتي","fixture"))
        show { ChatScreen({},vm) }
        ui.onNodeWithText("حذف عند الجميع").performClick();verify { vm.confirmDeleteForEveryone() }
        ui.onNodeWithText("حذف عندي فقط").performClick();verify { vm.confirmDeleteForMe() }
    }
}
