package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.trader.salesmanager.service.NotificationService
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class NotificationContractsTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private val manager get()=app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    @Before fun channels(){NotificationService.createChannels(app)}
    @After fun clear(){manager.cancelAll()}
    @Test fun createsDistinctAccountAndGeneralChannels() {
        assertNotNull(manager.getNotificationChannel("expiry_channel"));assertNotNull(manager.getNotificationChannel("general_channel"))
    }
    @Test fun blockedAndStatusMessagesPreserveTheirOwnContent() {
        NotificationService.showBlockedNotification(app,"DISABLED");NotificationService.showStatusChanged(app,"نشط")
        val rows=shadowOf(manager).allNotifications;assertEquals(2,rows.size)
        assertEquals(setOf("تم تعطيل حسابك من الإدارة","نشط"),rows.map{it.extras.getString("android.text")}.toSet())
    }
    @Test fun expiryWarningDistinguishesUpcomingAndExpiredAccount() {
        NotificationService.showExpiryWarning(app,3)
        assertTrue(shadowOf(manager).allNotifications.single().extras.getString("android.text")!!.contains("3 يوم"))
        NotificationService.showExpiryWarning(app,0)
        assertEquals(1,shadowOf(manager).allNotifications.size)
        assertTrue(shadowOf(manager).allNotifications.single().extras.getString("android.title")!!.contains("انتهى اشتراكك"))
    }
}
