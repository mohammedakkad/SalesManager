package com.trader.salesmanager.ui.qualification

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.trader.core.domain.model.StockLevel
import com.trader.core.domain.repository.*
import com.trader.salesmanager.MainActivity
import com.trader.salesmanager.worker.LowStockCheckWorker
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.*
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class LowStockWorkerRecoveryTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private val manager get()=app.getSystemService(NotificationManager::class.java)
    private val settings=mockk<LowStockSettingsRepository>();private val alerts=mockk<LowStockAlertRepository>()
    private val candidates=listOf(LowStockAlertCandidate("a","p",StockLevel.LOW),LowStockAlertCandidate("b","p",StockLevel.OUT),LowStockAlertCandidate("c","q",StockLevel.LOW))
    @Before fun setup(){
        coEvery{settings.getSettings()} returns LowStockAlertSettings();coEvery{alerts.getNotificationCandidates(any(),any())} returns candidates;coEvery{alerts.markNotified(any(),any())} just Runs
        startKoin{modules(module{single<LowStockSettingsRepository>{settings};single<LowStockAlertRepository>{alerts}})}
    }
    @After fun cleanup(){stopKoin();manager.cancelAll()}
    private fun worker()=TestListenableWorkerBuilder<LowStockCheckWorker>(app).build()
    private suspend fun cancellation(){try{worker().doWork();fail("Worker cancellation must propagate")}catch(expected:CancellationException){}}
    @Test fun cancellationWhileReadingSettingsDoesNotBecomeRetry()=runTest {
        coEvery{settings.getSettings()} throws CancellationException("settings cancelled");cancellation();coVerify(exactly=0){alerts.getNotificationCandidates(any(),any());alerts.markNotified(any(),any())}
    }
    @Test fun cancellationWhileReadingCandidatesDoesNotBecomeRetry()=runTest {
        coEvery{alerts.getNotificationCandidates(any(),any())} throws CancellationException("candidate cancelled");cancellation();coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun cancellationWhileMarkingDeliveryDoesNotBecomeSuccess()=runTest {
        coEvery{alerts.markNotified(any(),any())} throws CancellationException("ack cancelled");cancellation();assertEquals(1,shadowOf(manager).allNotifications.size)
    }
    @Test @Config(sdk=[33]) fun deniedNotificationPermissionDoesNotConsumeCandidateCooldown()=runTest {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(PackageManager.PERMISSION_DENIED,app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        assertEquals(Result.success(),worker().doWork());assertTrue(shadowOf(manager).allNotifications.isEmpty());coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun globallyDisabledNotificationsDoNotConsumeCandidateCooldown()=runTest {
        shadowOf(manager).setNotificationsEnabled(false);assertEquals(Result.success(),worker().doWork());assertTrue(shadowOf(manager).allNotifications.isEmpty());coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun disabledStockChannelDoesNotConsumeCandidateCooldown()=runTest {
        manager.createNotificationChannel(NotificationChannel("low_stock_alert_channel","disabled",NotificationManager.IMPORTANCE_NONE))
        assertEquals(Result.success(),worker().doWork());assertTrue(shadowOf(manager).allNotifications.isEmpty());coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun emptyCandidateSetDoesNotPostOrAcknowledgeAnything()=runTest {
        coEvery{alerts.getNotificationCandidates(any(),any())} returns emptyList();assertEquals(Result.success(),worker().doWork());assertTrue(shadowOf(manager).allNotifications.isEmpty());coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun deliveredNotificationUsesDistinctProductCountAndLowStockDestination()=runTest {
        assertEquals(Result.success(),worker().doWork());val n=shadowOf(manager).allNotifications.single();assertTrue(n.extras.getString(Notification.EXTRA_TEXT)!!.contains("2 منتجات"))
        val i=shadowOf(n.contentIntent).savedIntent;assertEquals(MainActivity.NAV_LOW_STOCK,i.getStringExtra(MainActivity.EXTRA_NAVIGATE_TO));assertTrue(shadowOf(n.contentIntent).flags and PendingIntent.FLAG_IMMUTABLE !=0)
        coVerify(exactly=1){alerts.markNotified(candidates,any())}
    }
    @Test fun failedCooldownPersistenceKeepsTheExistingDeliveredNotification()=runTest {
        coEvery{alerts.markNotified(any(),any())} throws IOException("offline database fault")
        assertEquals(Result.success(),worker().doWork());assertEquals(1,shadowOf(manager).allNotifications.size)
    }
    @Test @Config(sdk=[24]) fun legacyAndroidDeliversWithoutNotificationChannelSupport()=runTest {
        assertEquals(Result.success(),worker().doWork());assertEquals(1,shadowOf(manager).allNotifications.size);coVerify(exactly=1){alerts.markNotified(candidates,any())}
    }
}
