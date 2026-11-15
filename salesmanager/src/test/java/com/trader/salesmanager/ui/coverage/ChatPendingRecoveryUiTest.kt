package com.trader.salesmanager.ui.coverage

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.chat.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** SQLite close/reopen and a new real ViewModel, not an Android process-kill qualification. */
@RunWith(RobolectricTestRunner::class)
class ChatPendingRecoveryUiTest:OfflineSellerUiTest() {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val name="chat-pending-recovery.db"
    private lateinit var db:AppDatabase
    private var model:ChatViewModel?=null
    private val repo=mockk<ChatRepository>(relaxed=true)
    private val activation=mockk<ActivationRepository>(relaxed=true)
    private fun open() { db=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.build() }
    @Before fun setup() {
        context.deleteDatabase(name);open()
        coEvery { activation.getMerchantCode() } returns "tenant";every { repo.getMessages("tenant") } returns MutableStateFlow(emptyList())
    }
    @After fun close() {
        ui.runOnIdle { model?.viewModelScope?.cancel() };ui.waitForIdle();db.close();context.deleteDatabase(name)
    }
    private fun render() {
        ui.runOnIdle { model=owned(ChatViewModel(repo,activation,db.pendingMessageDao())) }
        show { ChatScreen({},model!!) }
        ui.waitUntil(10000) { ui.onAllNodesWithText("queued body").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun reopenedPendingMessageOffersExplicitRetryWithItsOriginalIdentity()=runBlocking {
        db.pendingMessageDao().insert(PendingMessageEntity("stable","tenant","queued body","store",isFailed=false));db.close();open()
        render();ui.onNodeWithText("إعادة").assertExists();ui.onNodeWithText("الإرسال غير مؤكد").assertExists()
        coEvery { repo.sendMessage(any(),any()) } coAnswers {
            assertEquals("tenant",firstArg<String>());assertEquals("stable",secondArg<ChatMessage>().id);assertEquals("queued body",secondArg<ChatMessage>().text)
        }
        ui.onNodeWithText("إعادة").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("queued body").fetchSemanticsNodes().isEmpty() }
        assertTrue(db.pendingMessageDao().getAll("tenant").first().isEmpty())
        coVerify(exactly=1) { repo.sendMessage(any(),any()) }
    }
    @Test fun activeRetryHidesRetryAndDismissUntilTheAttemptCompletes()=runBlocking {
        db.pendingMessageDao().insert(PendingMessageEntity("stable","tenant","queued body","store",isFailed=true))
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        coEvery { repo.sendMessage(any(),any()) } coAnswers { entered.complete(Unit);release.await() }
        render();ui.onNodeWithText("إعادة").performClick()
        ui.waitUntil(10000) { entered.isCompleted };ui.waitForIdle()
        ui.onNodeWithText("إعادة").assertDoesNotExist();ui.onNodeWithText("حذف").assertDoesNotExist()
        ui.onNodeWithText("queued body").assertExists()
        ui.runOnIdle { release.complete(Unit) }
        ui.waitUntil(10000) { ui.onAllNodesWithText("queued body").fetchSemanticsNodes().isEmpty() }
    }
}
