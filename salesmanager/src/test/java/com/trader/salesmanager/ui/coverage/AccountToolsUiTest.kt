package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.payments.*
import com.trader.salesmanager.ui.settings.sessions.*
import com.trader.salesmanager.ui.navigation.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountToolsUiTest:OfflineSellerUiTest() {
    @Test fun paymentMethodDialogAddsOnlyAfterConfirmation() {
        val vm=mockk<PaymentMethodsViewModel>(relaxed=true);every{vm.uiState} returns MutableStateFlow(PaymentMethodsUiState(listOf(PaymentMethod(1,"نقد"))))
        show{PaymentMethodsScreen({},vm)}
        ui.onNodeWithText("نقد").assertExists();ui.onAllNodes(hasClickAction()).onLast().performClick()
        ui.onNodeWithText("الاسم").performTextInput("بطاقة");verify(exactly=0){vm.addMethod(any())}
        ui.onNodeWithText("إضافة").performClick();verify{vm.addMethod("بطاقة")}
    }
    @Test fun activeDeviceRevokeUsesSessionIdAndBackIsPreserved() {
        val vm=mockk<SessionsViewModel>(relaxed=true);every{vm.activeSessions} returns MutableStateFlow(listOf(Session("stable","device","جهاز تجريبي","model",1,2)))
        var back=0;show{SessionsScreen({back++},vm)}
        ui.onNodeWithText("جهاز تجريبي").assertExists();ui.onNodeWithContentDescription("إنهاء الجلسة").performClick();verify{vm.revokeSession("stable")}
        ui.onNodeWithContentDescription("رجوع").performClick();assertEquals(1,back)
    }
    @Test fun blockedAccountDisplaysReasonAndOnlyExplicitRetryInvokesCallback() {
        var retries=0;show{BlockedScreen("انتهى الاشتراك\nتواصل مع الإدارة",true,{retries++})}
        ui.onNodeWithText("انتهى الاشتراك").assertExists();assertEquals(0,retries)
        ui.onNodeWithText("إعادة المحاولة").performClick();assertEquals(1,retries)
    }
}
