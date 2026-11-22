package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.lifecycle.Lifecycle
import com.trader.core.domain.repository.*
import com.trader.salesmanager.BuildConfig
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real settings controls and system permission state; installer/download actions stay offline. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class SettingsUpdateRefreshUiTest:AppThemeUiFixture(){
    private val vm=mockk<AppUpdateViewModel>(relaxed=true)
    private val state=MutableStateFlow(AppUpdateUiState())
    private val low=mockk<LowStockSettingsRepository>(relaxed=true)
    private val info=AppUpdateInfo(BuildConfig.VERSION_CODE+1,"9.8.7","https://example.invalid/offline-update",emptyList(),false)
    @Before fun model(){every{vm.uiState} returns state;every{low.settings} returns MutableStateFlow(LowStockAlertSettings());shadowOf(activity.packageManager).setCanRequestPackageInstalls(true)}
    @After fun permission(){shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)}
    private fun render(){showApp{SettingsScreen({},{},updateViewModel=vm,lowStockSettingsRepository=low)}}
    private fun updateCard()=ui.onNodeWithText("تحديث التطبيق").performScrollTo()
    @Test fun aPendingCheckBlocksRepeatedClicksAndItsErrorCanBeRetriedExplicitly(){
        state.value=AppUpdateUiState(isChecking=true);render()
        updateCard().assertIsNotEnabled();ui.onNodeWithText("جاري التحقق...").assertExists();dark(true)
        ui.runOnIdle{state.value=AppUpdateUiState(error="تعذر التحقق من التحديثات")}
        ui.onNodeWithText("تعذر التحقق من التحديثات").assertExists();dark(false)
        verify(exactly=0){vm.checkForUpdate();vm.startDownload();vm.installUpdate()}
        updateCard().assertIsEnabled().performClick();verify(exactly=1){vm.checkForUpdate()}
    }
    @Test fun aRefreshedCandidateReplacesTheVisibleVersionWithoutStartingADownload(){
        state.value=AppUpdateUiState(updateAvailable=true,updateInfo=info);render()
        updateCard();ui.onNodeWithText("يوجد تحديث — الإصدار 9.8.7").assertExists();dark(true)
        ui.runOnIdle{state.value=state.value.copy(updateInfo=info.copy(versionName="9.9.0"))}
        ui.onNodeWithText("يوجد تحديث — الإصدار 9.8.7").assertDoesNotExist();ui.onNodeWithText("يوجد تحديث — الإصدار 9.9.0").assertExists();dark(false)
        verify(exactly=0){vm.startDownload();vm.installUpdate()}
        ui.onNodeWithText("تحميل التحديث").performScrollTo().performClick();verify(exactly=1){vm.startDownload()}
    }
    @Test fun downloadProgressRemainsBusyAcrossThemeAndOnlyReadyStateOffersInstallation(){
        state.value=AppUpdateUiState(updateAvailable=true,updateInfo=info,downloadProgress=0);render()
        updateCard().assertIsNotEnabled();ui.onNodeWithText("0%").assertExists();dark(true)
        ui.runOnIdle{state.value=state.value.copy(downloadProgress=37)}
        ui.onNodeWithText("37%").assertExists();ui.onNodeWithText("تحميل التحديث").assertDoesNotExist();ui.onNodeWithText("تثبيت التحديث").assertDoesNotExist()
        dark(false);ui.runOnIdle{state.value=state.value.copy(downloadProgress=null,isReadyToInstall=true)}
        ui.onNodeWithText("اكتمل التحميل — اضغط للتثبيت").assertExists()
        verify(exactly=0){vm.installUpdate();vm.startDownload()}
        ui.onNodeWithText("تثبيت التحديث").performScrollTo().performClick();verify(exactly=1){vm.installUpdate()}
    }
    @Test fun deniedUnknownAppsPermissionOffersSettingsWithoutDownloadingOrInstalling(){
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)
        state.value=AppUpdateUiState(updateAvailable=true,updateInfo=info);render();updateCard()
        ui.onNodeWithText("يلزم منح إذن التثبيت").assertExists();dark(true)
        ui.onNodeWithText("تحميل التحديث").assertDoesNotExist();ui.onNodeWithText("تثبيت التحديث").assertDoesNotExist()
        ui.onNodeWithText("فتح إعدادات التثبيت").performScrollTo().performClick()
        verify(exactly=1){vm.openInstallSettings()};verify(exactly=0){vm.startDownload();vm.installUpdate()}
    }
    @Test fun grantingUnknownAppsPermissionOutsideTheActivityRefreshesActionsOnResume(){
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)
        state.value=AppUpdateUiState(updateAvailable=true,updateInfo=info);render();updateCard()
        ui.onNodeWithText("فتح إعدادات التثبيت").assertExists()
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(true)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        org.junit.Assert.assertTrue(AppUpdateDownloader.canInstallUnknownApps(activity))
        ui.onNodeWithText("تحميل التحديث").performScrollTo().assertExists()
        ui.onNodeWithText("فتح إعدادات التثبيت").assertDoesNotExist()
        verify(exactly=0){vm.startDownload();vm.installUpdate()}
    }
    @Test fun revokingUnknownAppsPermissionOutsideTheActivityRemovesTheInstallActionOnResume(){
        state.value=AppUpdateUiState(updateAvailable=true,updateInfo=info,isReadyToInstall=true);render();updateCard()
        ui.onNodeWithText("تثبيت التحديث").assertExists()
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        org.junit.Assert.assertFalse(AppUpdateDownloader.canInstallUnknownApps(activity))
        ui.onNodeWithText("فتح إعدادات التثبيت").performScrollTo().assertExists()
        ui.onNodeWithText("تثبيت التحديث").assertDoesNotExist()
        verify(exactly=0){vm.startDownload();vm.installUpdate()}
    }

}
