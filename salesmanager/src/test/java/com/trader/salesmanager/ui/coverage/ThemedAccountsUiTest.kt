package com.trader.salesmanager.ui.coverage

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.datastore.preferences.core.preferencesOf
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.activation.*
import com.trader.salesmanager.ui.navigation.BlockedScreen
import com.trader.salesmanager.ui.payments.*
import com.trader.salesmanager.ui.settings.*
import com.trader.salesmanager.ui.settings.sessions.*
import com.trader.salesmanager.ui.theme.DARK_MODE_KEY
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedAccountsUiTest:AppThemeUiFixture() {
    private fun settings() {
        val updates=mockk<AppUpdateViewModel>(relaxed=true)
        every{updates.uiState} returns MutableStateFlow(AppUpdateUiState())
        val low=mockk<LowStockSettingsRepository>(relaxed=true)
        every{low.settings} returns MutableStateFlow(LowStockAlertSettings())
        showApp{SettingsScreen({},{},updateViewModel=updates,lowStockSettingsRepository=low)}
    }
    @Test fun settingsThemeSwitchPersistsTheChoiceWithoutChangingStoreOrMerchant() {
        preferences.value=preferencesOf(STORE_NAME_KEY to "متجر ثابت",MERCHANT_CODE_KEY to "owner-A")
        settings()
        ui.onAllNodes(isToggleable()).assertCountEquals(2)
        ui.onAllNodes(isToggleable())[0].performClick()
        ui.onNodeWithText("الوضع الليلي مفعّل").assertExists()
        ui.runOnIdle{assertEquals(true,preferences.value[DARK_MODE_KEY])}
        ui.onAllNodes(isToggleable())[0].performClick()
        ui.onNodeWithText("الوضع النهاري مفعّل").assertExists()
        ui.runOnIdle {
            assertEquals(false,preferences.value[DARK_MODE_KEY])
            assertEquals("متجر ثابت",preferences.value[STORE_NAME_KEY])
            assertEquals("owner-A",preferences.value[MERCHANT_CODE_KEY])
        }
    }
    @Test fun merchantCopyUsesTheCurrentPreferenceAndDisablesAfterAccountCodeIsCleared() {
        preferences.value=preferencesOf(MERCHANT_CODE_KEY to "owner-A")
        settings();dark(true)
        ui.runOnIdle{preferences.value=preferences.value.toMutablePreferences().apply{this[MERCHANT_CODE_KEY]="owner-B"}}
        ui.onNodeWithText("owner-A").assertDoesNotExist()
        ui.onNodeWithContentDescription("نسخ كود التاجر").performClick()
        val clipboard=activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("owner-B",clipboard.primaryClip!!.getItemAt(0).text.toString())
        ui.runOnIdle{preferences.value=preferences.value.toMutablePreferences().apply{remove(MERCHANT_CODE_KEY)}}
        dark(false)
        ui.onNodeWithText("owner-B").assertDoesNotExist()
        ui.onNodeWithContentDescription("نسخ كود التاجر").assertIsNotEnabled()
    }
    @Test fun sessionRevocationRemainsBusyAcrossThemeAndRequiresExplicitRetryAfterFailure() {
        val vm=mockk<SessionsViewModel>(relaxed=true)
        val revoking=MutableStateFlow(emptySet<String>())
        val error=MutableStateFlow<String?>(null)
        every{vm.activeSessions} returns MutableStateFlow(listOf(Session("session","d","جهاز محفوظ","model",1,2)))
        every{vm.revoking} returns revoking;every{vm.revokeError} returns error
        every{vm.revokeSession("session")} answers{revoking.value=setOf("session")}
        showApp{SessionsScreen({},vm)}
        ui.onNodeWithContentDescription("إنهاء الجلسة").performClick();dark(true)
        ui.onNodeWithContentDescription("إنهاء الجلسة").assertIsNotEnabled()
        ui.runOnIdle{error.value="تعذر إنهاء الجلسة";revoking.value=emptySet()}
        dark(false)
        ui.onNodeWithText("تعذر إنهاء الجلسة").assertExists()
        verify(exactly=1){vm.revokeSession("session")}
        ui.onNodeWithContentDescription("إنهاء الجلسة").assertIsEnabled().performClick()
        verify(exactly=2){vm.revokeSession("session")}
    }
    @Test fun liveSessionReorderingRevokesTheNewVisibleDeviceIdentity() {
        val vm=mockk<SessionsViewModel>(relaxed=true)
        fun session(id:String,name:String)=Session(id,"d$id",name,"model",1,2)
        val sessions=MutableStateFlow(listOf(session("a","جهاز قديم"),session("b","جهاز باق")))
        every{vm.activeSessions} returns sessions
        every{vm.revoking} returns MutableStateFlow(emptySet())
        every{vm.revokeError} returns MutableStateFlow(null)
        showApp{SessionsScreen({},vm)};dark(true)
        ui.runOnIdle{sessions.value=listOf(session("b","جهاز باق"),session("c","جهاز جديد"))}
        ui.onNodeWithText("جهاز قديم").assertDoesNotExist();ui.onNodeWithText("جهاز جديد").assertExists()
        ui.onAllNodesWithContentDescription("إنهاء الجلسة").assertCountEquals(2)
        ui.onAllNodesWithContentDescription("إنهاء الجلسة")[1].performClick()
        verify(exactly=1){vm.revokeSession("c")};verify(exactly=0){vm.revokeSession("a")}
    }
    @Test fun paymentMethodReplacementDeletesTheCurrentRowAndDoesNotAddARecord() {
        val vm=mockk<PaymentMethodsViewModel>(relaxed=true)
        val rows=MutableStateFlow(PaymentMethodsUiState(listOf(PaymentMethod(4,"طريقة قديمة"),PaymentMethod(7,"بطاقة"))))
        every{vm.uiState} returns rows
        showApp{PaymentMethodsScreen({},vm)};dark(true)
        val updated=PaymentMethod(7,"بطاقة جديدة")
        ui.runOnIdle{rows.value=PaymentMethodsUiState(listOf(updated,PaymentMethod(9,"نقد جديد")))}
        ui.onNodeWithText("طريقة قديمة").assertDoesNotExist()
        ui.onNodeWithText("بطاقة جديدة").onParent().onChildren().filter(hasClickAction()).onFirst().performClick()
        verify(exactly=1){vm.deleteMethod(updated)};verify(exactly=0){vm.addMethod(any())}
    }
    @Test fun pendingActivationKeepsCodeAndSubmitDisabledUntilAnExplicitFailureRetry() {
        val vm=mockk<ActivationViewModel>(relaxed=true)
        val state=MutableStateFlow(ActivationUiState())
        every{vm.uiState} returns state
        every{vm.updateCode(any())} answers{state.value=state.value.copy(code=firstArg())}
        every{vm.activate()} answers{state.value=state.value.copy(isLoading=true,error=null)}
        var activated=0
        showApp{ActivationScreen({activated++},{},vm)}
        ui.onNode(hasSetTextAction()).performTextInput("fixture-code")
        ui.onNodeWithText("تفعيل التطبيق").performClick();dark(true)
        ui.onNodeWithText("جاري التحقق...").assertIsNotEnabled()
        ui.onNode(hasText("fixture-code") and SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).assertIsNotEnabled()
        ui.runOnIdle{state.value=state.value.copy(isLoading=false,error="تعذر التحقق")}
        dark(false)
        ui.onNodeWithText("تعذر التحقق").assertExists()
        ui.onNode(hasSetTextAction()).assertTextContains("fixture-code").assertIsEnabled()
        verify(exactly=1){vm.activate()};assertEquals(0,activated)
        ui.onNodeWithText("تفعيل التطبيق").assertIsEnabled().performClick()
        verify(exactly=2){vm.activate()}
    }
    @Test fun confirmedActivationDoesNotNavigateAgainOnThemeOrUnrelatedStateChange() {
        val vm=mockk<ActivationViewModel>(relaxed=true)
        val state=MutableStateFlow(ActivationUiState(code="fixture-code"))
        every{vm.uiState} returns state;var navigations=0
        showApp{ActivationScreen({navigations++},{},vm)}
        ui.runOnIdle{state.value=state.value.copy(isSuccess=true)}
        ui.waitUntil(3000){navigations==1}
        dark(true)
        ui.runOnIdle{state.value=state.value.copy(code="changed-after-success")}
        dark(false)
        ui.runOnIdle{assertEquals(1,navigations)};verify(exactly=0){vm.activate()}
    }
    @Test fun blockedAccountRetryAppearsOnlyWhenPermittedAndNeverRunsFromThemeChange() {
        val canRetry=mutableStateOf(false);val reason=mutableStateOf("الحساب موقوف\nتواصل مع الإدارة")
        var retries=0
        showApp{BlockedScreen(reason.value,canRetry.value,{retries++})}
        ui.onNodeWithText("إعادة المحاولة").assertDoesNotExist();dark(true)
        ui.runOnIdle{canRetry.value=true;reason.value="انتهى الاشتراك\nتواصل مع الإدارة"}
        ui.onNodeWithText("الحساب موقوف").assertDoesNotExist();ui.onNodeWithText("انتهى الاشتراك").assertExists()
        dark(false);assertEquals(0,retries)
        ui.onNodeWithText("إعادة المحاولة").performClick();assertEquals(1,retries)
    }
}
