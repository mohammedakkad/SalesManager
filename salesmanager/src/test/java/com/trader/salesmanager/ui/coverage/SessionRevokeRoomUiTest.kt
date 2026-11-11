package com.trader.salesmanager.ui.coverage

import android.content.Context
import androidx.compose.ui.test.*
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.SessionEntity
import com.trader.salesmanager.ui.settings.sessions.SessionsScreen
import com.trader.salesmanager.ui.settings.sessions.SessionsViewModel
import io.mockk.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRevokeRoomUiTest:OfflineSellerUiTest() {
    private lateinit var db:AppDatabase
    private lateinit var vm:SessionsViewModel
    private val reference=mockk<DatabaseReference>(relaxed=true)
    @Before fun seed()=runBlocking {
        val context:Context=ApplicationProvider.getApplicationContext()
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        db.sessionDao().insertSession(SessionEntity("session-a","device-a","OFFLINE-DEVICE",loginDate=1,lastActive=2))
        val database=mockk<FirebaseDatabase>(relaxed=true)
        mockkStatic(FirebaseDatabase::class)
        every { FirebaseDatabase.getInstance() } returns database
        every { database.getReference("merchants").child("merchant-a").child("sessions").child("session-a") } returns reference
        ui.runOnIdle {vm=owned(SessionsViewModel(db.sessionDao(),"merchant-a"))}
    }
    @After fun close(){ui.runOnIdle {vm.viewModelScope.cancel()};db.close();unmockkStatic(FirebaseDatabase::class)}
    private fun display(expected:Int=1){show {SessionsScreen({},vm)};ui.waitUntil(10000){ui.waitForIdle();vm.activeSessions.value.size==expected};ui.waitForIdle()}
    private fun awaitFailure(){
        // Room work finishes outside Compose idling. Wait for the actual failure
        // and released request, then retain the full displayed-error assertion.
        ui.waitUntil(10000){ui.waitForIdle();vm.revokeError.value!=null&&vm.revoking.value.isEmpty()}
        ui.waitForIdle()
    }
    private fun awaitEmpty(){ui.waitUntil(10000){ui.waitForIdle();vm.activeSessions.value.isEmpty()}}
    @Test fun failedRemoteRevokeKeepsRealLocalSessionAndDisplaysFailure() {
        every { reference.removeValue() } returns Tasks.forException<Void>(java.io.IOException("offline fixture"))
        display()
        ui.onNodeWithContentDescription("إنهاء الجلسة").performClick()
        awaitFailure()
        ui.onNodeWithText("تعذر إنهاء الجلسة، حاول مجددًا").assertIsDisplayed()
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
    }
    @Test fun pendingRevokeDisablesAnotherSubmissionUntilAcknowledged() {
        val task=TaskCompletionSource<Void>()
        every { reference.removeValue() } returns task.task
        display()
        ui.onNodeWithContentDescription("إنهاء الجلسة").performClick()
        ui.onNodeWithContentDescription("إنهاء الجلسة").assertIsNotEnabled()
        verify(exactly=1){reference.removeValue()}
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
        ui.runOnIdle { task.setResult(null) }
        try { ui.waitUntil(10000){ui.waitForIdle();vm.activeSessions.value.isEmpty()} }
        catch (timeout:ComposeTimeoutException) {
            val present=runBlocking {db.sessionDao().getById("session-a")!=null}
            throw AssertionError("Session acknowledgment did not settle: revoking=${vm.revoking.value} error=${vm.revokeError.value} rowPresent=$present",timeout)
        }
        ui.onNodeWithText("لا توجد جلسات نشطة حالياً").assertIsDisplayed()
    }

    @Test fun rejectedRevokeCanBeRetriedWithoutDeletingLocalSessionEarly() {
        every { reference.removeValue() } returns Tasks.forException<Void>(SecurityException("offline permission fixture"))
        display();ui.onNodeWithContentDescription("إنهاء الجلسة").performClick()
        awaitFailure()
        ui.onNodeWithText("تعذر إنهاء الجلسة، حاول مجددًا").assertIsDisplayed()
        ui.onNodeWithContentDescription("إنهاء الجلسة").assertIsEnabled()
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
        every { reference.removeValue() } returns Tasks.forResult<Void>(null)
        ui.onNodeWithContentDescription("إنهاء الجلسة").performClick();awaitEmpty()
        ui.onNodeWithText("تعذر إنهاء الجلسة، حاول مجددًا").assertDoesNotExist()
        runBlocking {assertNull(db.sessionDao().getById("session-a"))}
        verify(exactly=2){reference.removeValue()}
    }
    @Test fun duplicateDirectCallsShareOneInFlightRemoval() {
        val task=TaskCompletionSource<Void>()
        every {reference.removeValue()} returns task.task
        display()
        ui.runOnIdle {vm.revokeSession("session-a");vm.revokeSession("session-a")}
        verify(exactly=1){reference.removeValue()}
        assertEquals(setOf("session-a"),vm.revoking.value)
        ui.runOnIdle {task.setResult(null)};awaitEmpty()
        assertTrue(vm.revoking.value.isEmpty())
    }
    @Test fun cancellationBeforeRemoteCompletionKeepsLocalRecordWithoutFalseError() {
        val task=TaskCompletionSource<Void>()
        every {reference.removeValue()} returns task.task
        display();ui.onNodeWithContentDescription("إنهاء الجلسة").performClick()
        ui.runOnIdle {vm.viewModelScope.cancel();task.setResult(null)}
        ui.waitForIdle()
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
        assertNull(vm.revokeError.value);assertTrue(vm.revoking.value.isEmpty())
    }
    @Test fun localDeleteFailureAfterRemoteAcknowledgmentIsVisibleAndRetryable() {
        every {reference.removeValue()} returns Tasks.forResult<Void>(null)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER injected BEFORE DELETE ON sessions BEGIN SELECT RAISE(ABORT,'offline fault'); END")
        display();ui.onNodeWithContentDescription("إنهاء الجلسة").performClick()
        awaitFailure()
        ui.onNodeWithText("تعذر إنهاء الجلسة، حاول مجددًا").assertIsDisplayed()
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER injected")
        ui.onNodeWithContentDescription("إنهاء الجلسة").performClick();awaitEmpty()
        runBlocking {assertNull(db.sessionDao().getById("session-a"))}
        verify(exactly=2){reference.removeValue()}
    }
    @Test fun blankIdentityOrMerchantCannotIssueRemoval() {
        display()
        ui.runOnIdle {
            vm.revokeSession("")
            val unactivated=owned(SessionsViewModel(db.sessionDao(),""))
            unactivated.revokeSession("session-a");unactivated.viewModelScope.cancel()
        }
        verify(exactly=0){reference.removeValue()}
        runBlocking {assertNotNull(db.sessionDao().getById("session-a"))}
        assertNull(vm.revokeError.value)
    }
    @Test fun realSessionStreamSortsActivityAndDisplaysUnknownDeviceFallback() {
        runBlocking {db.sessionDao().insertSession(SessionEntity("session-b","device-b","",deviceModel="MODEL-B",loginDate=2,lastActive=3))}
        display(2)
        assertEquals(listOf("session-b","session-a"),vm.activeSessions.value.map{it.id})
        assertEquals("MODEL-B",vm.activeSessions.value.first().deviceModel)
        ui.onNodeWithText("جهاز غير معروف").assertIsDisplayed()
        verify(exactly=0){reference.removeValue()}
    }
}
