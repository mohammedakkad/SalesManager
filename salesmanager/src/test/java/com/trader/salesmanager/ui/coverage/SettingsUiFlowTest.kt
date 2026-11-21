package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.settings.SettingsScreen
import com.trader.salesmanager.update.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsUiFlowTest:OfflineSellerUiTest() {
    private val vm=mockk<AppUpdateViewModel>(relaxed=true)
    private val updates=MutableStateFlow(AppUpdateUiState())
    private val low=mockk<LowStockSettingsRepository>(relaxed=true)
    @Before fun data() {
        every { vm.uiState } returns updates
        every { low.settings } returns MutableStateFlow(LowStockAlertSettings())
    }
    @Test fun paymentCashAndSupportSettingsNavigateToTheRequestedDestination() {
        var destination=""
        show { SettingsScreen({}, {destination="payments"},{destination="cash"},{destination="support"},updateViewModel=vm,lowStockSettingsRepository=low) }
        ui.onNodeWithText("طرق الدفع").performScrollTo().performClick();Assert.assertEquals("payments",destination)
        ui.onNodeWithText("الصناديق").performScrollTo().performClick();Assert.assertEquals("cash",destination)
        ui.onNodeWithText("الدعم الفني").performScrollTo().performClick();Assert.assertEquals("support",destination)
    }
    @Test fun editingStoreNameRequiresExplicitSave() {
        show { SettingsScreen({}, {},updateViewModel=vm,lowStockSettingsRepository=low) }
        ui.onNodeWithText("اسم المحل").performScrollTo().performClick()
        ui.onNode(hasSetTextAction()).performTextReplacement("متجر الاختبار")
        ui.onNodeWithText("إلغاء").performClick()
        ui.onNodeWithText("متجر الاختبار").assertDoesNotExist()
    }
}
