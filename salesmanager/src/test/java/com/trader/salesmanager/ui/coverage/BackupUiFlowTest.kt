package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.salesmanager.ui.settings.backup.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupUiFlowTest:OfflineSellerUiTest() {
    private val vm=mockk<BackupViewModel>(relaxed=true)
    private val state=MutableStateFlow(BackupUiState())
    @Before fun data() { every { vm.uiState } returns state }
    @Test fun exportingRequiresPasswordAndConfirmationBeforeDelegation() {
        every { vm.requestExport() } answers { state.value=state.value.copy(showExportPasswordDialog=true) }
        show { BackupScreen({},viewModel=vm) }
        ui.onNodeWithText("تصدير نسخة احتياطية").performScrollTo().performClick()
        ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("fixture-password")
        ui.onAllNodes(hasSetTextAction())[1].performTextReplacement("fixture-password")
        ui.onNodeWithText("تصدير ومشاركة").performClick()
        verify { vm.confirmExportPassword("fixture-password","fixture-password") }
    }
    @Test fun restoringRequiresExplicitReplacementConfirmation() {
        state.value=BackupUiState(showImportConfirmDialog=true,pendingImport=BackupPayload(exportedAt=1))
        show { BackupScreen({},viewModel=vm) }
        ui.onNodeWithText("نعم، استبدال البيانات").performClick()
        verify(exactly=1) { vm.confirmImport() }
    }
}
