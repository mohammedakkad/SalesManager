package com.trader.core.worker

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.data.local.appDataStore
import com.trader.core.data.remote.FirebaseSyncService
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class StatusCheckWorkerBoundaryTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private val activated=booleanPreferencesKey("is_activated")
    private val code=stringPreferencesKey("merchant_code")
    private val storeName=stringPreferencesKey("store_name")
    private val prefs=MutableStateFlow<Preferences>(preferencesOf(activated to true,code to "old",storeName to "متجر"))
    private val store=object:DataStore<Preferences>{
        override val data:Flow<Preferences> = prefs
        override suspend fun updateData(transform:suspend(Preferences)->Preferences):Preferences=transform(prefs.value).also{prefs.value=it}
    }
    private val manager get()=app.getSystemService(NotificationManager::class.java)
    @Before fun setup(){
        mockkStatic("com.trader.core.data.local.DataStoreProviderKt");every{any<Context>().appDataStore} returns store
        // The constructor's SDK dependency and status response are both offline.
        mockkStatic(FirebaseDatabase::class);every{FirebaseDatabase.getInstance()} returns mockk(relaxed=true)
        mockkConstructor(FirebaseSyncService::class)
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returns "ACTIVE"
        manager.cancelAll()
    }
    @After fun cleanup(){unmockkConstructor(FirebaseSyncService::class);unmockkStatic(FirebaseDatabase::class);unmockkStatic("com.trader.core.data.local.DataStoreProviderKt");manager.cancelAll()}
    private fun worker()=TestListenableWorkerBuilder<StatusCheckWorker>(app).build()
    private fun session(active:Boolean,merchant:String){prefs.value=preferencesOf(activated to active,code to merchant,storeName to "متجر")}
    @Test fun staleDisabledResponseCannotClearTheNewMerchantOrNotify()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} coAnswers {session(true,"new");"DISABLED"}
        assertEquals(Result.success(),worker().doWork());assertEquals("new",prefs.value[code]);assertEquals(true,prefs.value[activated]);assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
    @Test fun staleExpiredResponseAfterLogoutCannotPostAFalseAccountNotification()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} coAnswers {session(false,"");"EXPIRED"}
        assertEquals(Result.success(),worker().doWork());assertEquals("",prefs.value[code]);assertFalse(prefs.value[activated]!!);assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
    @Test fun disabledAccountClearsOnlyActivationAndPostsItsSpecificReason()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returns "DISABLED"
        assertEquals(Result.success(),worker().doWork());assertEquals(false,prefs.value[activated]);assertEquals("",prefs.value[code]);assertEquals("متجر",prefs.value[storeName])
        val n=shadowOf(manager).allNotifications.single();assertEquals("⚠️ تم تعطيل حسابك",n.extras.getString(Notification.EXTRA_TITLE));assertEquals("تواصل مع الإدارة لإعادة التفعيل",n.extras.getString(Notification.EXTRA_TEXT));assertNotNull(manager.getNotificationChannel("status_channel"))
    }
    @Test fun expiredAccountUsesRenewalMessage()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returns "EXPIRED"
        assertEquals(Result.success(),worker().doWork());assertEquals(false,prefs.value[activated]);assertEquals("⚠️ انتهت مدة اشتراكك",shadowOf(manager).allNotifications.single().extras.getString(Notification.EXTRA_TITLE));assertEquals("تواصل مع الإدارة لتجديد الاشتراك",shadowOf(manager).allNotifications.single().extras.getString(Notification.EXTRA_TEXT))
    }
    @Test @Config(sdk=[24]) fun deletedAccountNotifiesOnLegacyAndroidWithoutAChannel()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returns "DELETED"
        assertEquals(Result.success(),worker().doWork());assertEquals(false,prefs.value[activated]);assertEquals("⚠️ تم حذف حسابك",shadowOf(manager).allNotifications.single().extras.getString(Notification.EXTRA_TITLE))
    }
    @Test fun unavailableStatusRetriesWithoutChangingSessionOrNotifying()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returns null
        assertEquals(Result.retry(),worker().doWork());assertEquals("old",prefs.value[code]);assertEquals(true,prefs.value[activated]);assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
    @Test fun activeAndUnrecognizedStatusesLeaveTheSessionAndNotificationsAlone()=runBlocking {
        coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} returnsMany listOf("ACTIVE","future-status")
        assertEquals(Result.success(),worker().doWork());assertEquals(Result.success(),worker().doWork());assertEquals("old",prefs.value[code]);assertEquals(true,prefs.value[activated]);assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
    @Test fun inactiveOrMissingCodeSkipsTheRemoteRead()=runBlocking {
        session(false,"old");assertEquals(Result.success(),worker().doWork());session(true,"");assertEquals(Result.success(),worker().doWork())
        coVerify(exactly=0){anyConstructed<FirebaseSyncService>().getCodeStatus(any())};assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
    @Test fun cancelledStatusReadPropagatesWithoutInvalidatingTheSession()=runBlocking {
        val cancelled=CancellationException("offline provider cancellation");coEvery{anyConstructed<FirebaseSyncService>().getCodeStatus("old")} throws cancelled
        try{worker().doWork();fail("Cancellation lost")}catch(actual:CancellationException){assertSame(cancelled,actual)}
        assertEquals("old",prefs.value[code]);assertEquals(true,prefs.value[activated]);assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}
