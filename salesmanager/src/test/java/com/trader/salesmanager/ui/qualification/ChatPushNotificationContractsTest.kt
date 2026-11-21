package com.trader.salesmanager.ui.qualification

import android.app.*
import android.content.*
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.RemoteMessage
import com.trader.salesmanager.ChatNotificationService
import com.trader.salesmanager.MainActivity
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real FCM message decoding and Android service notifications, without token/network startup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ChatPushNotificationContractsTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private val manager get()=app.getSystemService(NotificationManager::class.java)
    private lateinit var controller:org.robolectric.android.controller.ServiceController<ChatNotificationService>
    @Before fun setup(){controller=Robolectric.buildService(ChatNotificationService::class.java).create()}
    @After fun cleanup(){controller.destroy();manager.cancelAll()}
    private fun deliver(vararg entries:Pair<String,String>):Notification {
        controller.get().onMessageReceived(RemoteMessage(Bundle().apply{entries.forEach{putString(it.first,it.second)}}))
        return shadowOf(manager).getNotification(ChatNotificationService.CHAT_NOTIF_ID)!!
    }
    @Test fun dataMessagePreservesArabicTitleBodyAndTimestampInExpandedText(){
        val n=deliver("title" to "الدعم","body" to "رسالة عربية","sentAt" to "10:30")
        assertEquals("الدعم",n.extras.getString(Notification.EXTRA_TITLE));assertEquals("رسالة عربية\n🕐 10:30",n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
        assertEquals(n.extras.getString(Notification.EXTRA_TEXT),n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
    }
    @Test fun notificationPayloadTakesPrecedenceOverDataFallback(){
        val n=deliver("gcm.n.e" to "1","gcm.n.title" to "عنوان FCM","gcm.n.body" to "محتوى FCM","title" to "احتياطي","body" to "احتياطي")
        assertEquals("عنوان FCM",n.extras.getString(Notification.EXTRA_TITLE));assertEquals("محتوى FCM",n.extras.getString(Notification.EXTRA_TEXT))
    }
    @Test fun absentPayloadUsesTheExistingArabicDefaultAndEmptyBody(){
        val n=deliver();assertEquals("رسالة جديدة من الإدارة",n.extras.getString(Notification.EXTRA_TITLE));assertEquals("",n.extras.getString(Notification.EXTRA_TEXT))
    }
    @Test fun emptyTimestampDoesNotAddAnExtraLine(){assertEquals("نص",deliver("body" to "نص","sentAt" to "").extras.getString(Notification.EXTRA_TEXT))}
    @Test fun channelUsesHighImportanceAndExistingDescription(){
        deliver();val c=manager.getNotificationChannel(ChatNotificationService.CHAT_CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH,c.importance);assertEquals("رسائل الدعم الفني",c.name.toString());assertEquals("رسائل من إدارة التطبيق",c.description);assertTrue(c.shouldVibrate());assertTrue(c.shouldShowLights())
    }
    @Test fun tapRoutesToTheChatDestinationWithAnImmutablePendingIntent(){
        val n=deliver();val pending=shadowOf(n.contentIntent);val i=pending.savedIntent
        assertEquals(MainActivity::class.java.name,i.component!!.className);assertEquals("chat",i.getStringExtra("navigate_to"))
        assertTrue(i.flags and Intent.FLAG_ACTIVITY_NEW_TASK !=0);assertTrue(i.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP !=0)
        assertTrue(pending.flags and PendingIntent.FLAG_IMMUTABLE !=0);assertTrue(n.flags and Notification.FLAG_AUTO_CANCEL !=0)
    }
    @Test fun laterChatNotificationReplacesTheExistingChatSlot(){
        deliver("body" to "الأولى");deliver("body" to "الثانية");assertEquals(1,shadowOf(manager).allNotifications.size)
        assertEquals("الثانية",shadowOf(manager).allNotifications.single().extras.getString(Notification.EXTRA_TEXT))
    }
    @Test @Config(sdk=[24]) fun legacyAndroidReceivesTheSameContentWithoutAChannel(){
        val n=deliver("title" to "قديم","body" to "API24");assertEquals("قديم",n.extras.getString(Notification.EXTRA_TITLE));assertEquals("API24",n.extras.getString(Notification.EXTRA_TEXT));assertNotNull(n.contentIntent)
    }
}
