package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.trader.salesmanager.BuildConfig
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class UpdateLifecycleTest {
    private val app:Application=ApplicationProvider.getApplicationContext();private val store=ViewModelStore()
    private val info=AppUpdateInfo(BuildConfig.VERSION_CODE+1,"fixture","https://example.invalid/offline-fixture",listOf("fixture"),false)
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());mockkObject(AppUpdateChecker,AppUpdateDownloader);app.getSharedPreferences(AppUpdateViewModel.PREFS_NAME,0).edit().clear().commit()}
    @After fun cleanup(){store.clear();unmockkObject(AppUpdateChecker,AppUpdateDownloader);Dispatchers.resetMain();(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()}
    private fun model()=AppUpdateViewModel(app).also{store.put("update",it)}
    @Test fun checkerFailureAndCurrentVersionNeverOfferAnUpdate()=runTest {
        coEvery{AppUpdateChecker.check()} returns null;val vm=model();vm.checkForUpdate();runCurrent();assertFalse(vm.uiState.value.isChecking);assertNotNull(vm.uiState.value.error)
        coEvery{AppUpdateChecker.check()} returns info.copy(latestVersion=BuildConfig.VERSION_CODE)
        vm.checkForUpdate();runCurrent();assertFalse(vm.uiState.value.updateAvailable);assertNull(vm.uiState.value.updateInfo);assertNull(vm.uiState.value.error)
    }
    @Test fun overlappingChecksAreSuppressedAndNewerVersionIsPresented()=runTest {
        coEvery{AppUpdateChecker.check()} coAnswers {delay(100);info};val vm=model();vm.checkForUpdate();vm.checkForUpdate();advanceUntilIdle()
        coVerify(exactly=1){AppUpdateChecker.check()};assertEquals(info,vm.uiState.value.updateInfo);assertTrue(vm.uiState.value.updateAvailable)
    }
    @Test fun downloadedPathSurvivesViewModelRestartAndInstallUsesThatExactFile()=runTest {
        coEvery{AppUpdateChecker.check()} returns info
        val file=File(app.cacheDir,"download-transport.fixture").apply{writeText("transport fixture, not a qualified APK")}
        coEvery{AppUpdateDownloader.downloadApk(any(),any(),any())} coAnswers {arg<(Int)->Unit>(2)(50);file}
        every{AppUpdateDownloader.installApk(any(),any())} just Runs
        val vm=model();vm.checkForUpdate();runCurrent();vm.startDownload();runCurrent();assertTrue(vm.uiState.value.isReadyToInstall)
        assertEquals(file.absolutePath,app.getSharedPreferences(AppUpdateViewModel.PREFS_NAME,0).getString(AppUpdateViewModel.KEY_APK_PATH,null))
        val restarted=AppUpdateViewModel(app).also{store.put("restarted",it)};restarted.markReadyToInstallFromBackground();assertTrue(restarted.uiState.value.isReadyToInstall)
        restarted.installUpdate();verify{AppUpdateDownloader.installApk(app,file)};file.delete()
    }
    @Test fun failedDownloadClearsProgressAndCanBeRetried()=runTest {
        coEvery{AppUpdateChecker.check()} returns info;coEvery{AppUpdateDownloader.downloadApk(any(),any(),any())} returns null
        val vm=model();vm.startDownload();coVerify(exactly=0){AppUpdateDownloader.downloadApk(any(),any(),any())}
        vm.checkForUpdate();runCurrent();vm.startDownload();runCurrent();assertNull(vm.uiState.value.downloadProgress);assertFalse(vm.uiState.value.isReadyToInstall);assertNotNull(vm.uiState.value.error)
        vm.startDownload();runCurrent();coVerify(exactly=2){AppUpdateDownloader.downloadApk(any(),any(),any())}
    }
    @Test fun backgroundCheckNotifiesOnlyForNewerRelease()=runTest {
        coEvery{AppUpdateChecker.check()} returns info.copy(latestVersion=BuildConfig.VERSION_CODE)
        val manager=app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(Result.success(),TestListenableWorkerBuilder<BackgroundUpdateWorker>(app).build().doWork());assertTrue(shadowOf(manager).allNotifications.isEmpty())
        coEvery{AppUpdateChecker.check()} returns info
        assertEquals(Result.success(),TestListenableWorkerBuilder<BackgroundUpdateWorker>(app).build().doWork());assertEquals(1,shadowOf(manager).allNotifications.size)
        assertTrue(shadowOf(manager).allNotifications.single().extras.getString("android.title")!!.contains("fixture"))
    }
    @Test fun backgroundDownloadFailureDoesNotPersistAnInstallPath()=runTest {
        coEvery{AppUpdateDownloader.downloadApk(any(),any(),any())} returns null
        val data=Data.Builder().putString(BackgroundUpdateWorker.KEY_URL,info.downloadUrl).build()
        assertEquals(Result.failure(),TestListenableWorkerBuilder<BackgroundUpdateWorker>(app).setInputData(data).build().doWork())
        assertNull(app.getSharedPreferences(AppUpdateViewModel.PREFS_NAME,0).getString(AppUpdateViewModel.KEY_APK_PATH,null))
    }
    @Test fun backgroundDownloadSuccessPersistsPathAndPostsReadyNotification()=runTest {
        val file=File(app.cacheDir,"background-transport.fixture").apply{writeText("offline transport fixture")}
        coEvery{AppUpdateDownloader.downloadApk(any(),any(),any())} coAnswers {arg<(Int)->Unit>(2)(50);file}
        val data=Data.Builder().putString(BackgroundUpdateWorker.KEY_URL,info.downloadUrl).putString(BackgroundUpdateWorker.KEY_VERSION_NAME,info.versionName).build()
        assertEquals(Result.success(),TestListenableWorkerBuilder<BackgroundUpdateWorker>(app).setInputData(data).build().doWork())
        assertEquals(file.absolutePath,app.getSharedPreferences(AppUpdateViewModel.PREFS_NAME,0).getString(AppUpdateViewModel.KEY_APK_PATH,null))
        val manager=app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(manager).allNotifications.any{it.extras.getString("android.title").orEmpty().contains("جاهز")});file.delete()
    }
}
