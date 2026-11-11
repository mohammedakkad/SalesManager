package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.trader.core.domain.model.StockLevel
import com.trader.core.domain.repository.*
import com.trader.salesmanager.worker.LowStockCheckWorker
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.*
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class LowStockWorkerBehaviorTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val settings=mockk<LowStockSettingsRepository>(relaxed=true);private val alerts=mockk<LowStockAlertRepository>(relaxed=true)
    @Before fun setup(){coEvery{settings.getSettings()} returns LowStockAlertSettings();coEvery{alerts.getNotificationCandidates(any(),any())} returns emptyList();startKoin{modules(module{single<LowStockSettingsRepository>{settings};single<LowStockAlertRepository>{alerts}})}}
    @After fun cleanup(){stopKoin();(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()}
    private fun worker()=TestListenableWorkerBuilder<LowStockCheckWorker>(context).build()
    @Test fun disabledSettingsDoNotQueryOrAcknowledgeCandidates()=runTest {
        coEvery{settings.getSettings()} returns LowStockAlertSettings(enabled=false)
        assertEquals(Result.success(),worker().doWork());coVerify(exactly=0){alerts.getNotificationCandidates(any(),any());alerts.markNotified(any(),any())}
    }
    @Test fun unavailableSettingsOrInventoryRequestsRetryWithoutAcknowledgment()=runTest {
        coEvery{settings.getSettings()} throws java.io.IOException("fixture")
        assertEquals(Result.retry(),worker().doWork());coEvery{settings.getSettings()} returns LowStockAlertSettings()
        coEvery{alerts.getNotificationCandidates(any(),any())} throws java.io.IOException("fixture")
        assertEquals(Result.retry(),worker().doWork());coVerify(exactly=0){alerts.markNotified(any(),any())}
    }
    @Test fun notificationCountsDistinctProductsAndOnlyThenMarksCandidates()=runTest {
        val candidates=listOf(LowStockAlertCandidate("a","p",StockLevel.LOW),LowStockAlertCandidate("b","p",StockLevel.OUT))
        coEvery{alerts.getNotificationCandidates(any(),any())} returns candidates
        assertEquals(Result.success(),worker().doWork());coVerify(exactly=1){alerts.markNotified(candidates,any())}
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(manager).allNotifications.single().extras.getString("android.text")!!.contains("1 منتجات"))
    }
}
