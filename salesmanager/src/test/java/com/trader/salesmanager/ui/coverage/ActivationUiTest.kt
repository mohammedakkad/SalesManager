package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.salesmanager.ui.activation.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivationUiTest:OfflineSellerUiTest() {
    @Test fun emptyCodeCannotActivateAndSuccessNavigatesOnlyAfterStateConfirms() {
        val vm=mockk<ActivationViewModel>(relaxed=true);val state=MutableStateFlow(ActivationUiState())
        every { vm.uiState } returns state
        var activated=0
        show { ActivationScreen({activated++},{},vm) }
        ui.onNode(hasSetTextAction()).performTextInput("fixture");verify { vm.updateCode("fixture") }
        assertEquals(0,activated)
        ui.runOnIdle { state.value=ActivationUiState(code="fixture",isSuccess=true) }
        ui.waitUntil(3000) { activated==1 }
        verify(exactly=0) { vm.activate() }
    }
    @Test fun activationFailureIsVisibleAndDoesNotNavigate() {
        val vm=mockk<ActivationViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(ActivationUiState(code="invalid",error="كود غير صالح"))
        var activated=0;show { ActivationScreen({activated++},{},vm) }
        ui.onNodeWithText("كود غير صالح").assertExists();assertEquals(0,activated)
    }
}
