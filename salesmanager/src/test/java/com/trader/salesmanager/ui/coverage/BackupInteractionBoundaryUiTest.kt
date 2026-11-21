package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.settings.backup.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** Real Compose controls and recomposition; no backup archive or synthetic PDF is generated. */
@RunWith(RobolectricTestRunner::class)
class BackupInteractionBoundaryUiTest : OfflineSellerUiTest() {
    private val vm=mockk<BackupViewModel>(relaxed=true)
    private val state=MutableStateFlow(BackupUiState())
    @Before fun model() { every { vm.uiState } returns state }
    private fun render() { show { BackupScreen({},viewModel=vm) } }
    private fun text(id:Int)=ui.onNodeWithText(activity.getString(id))
    @Test fun exportConfirmationTracksBothNonblankPasswordFieldsAcrossEdits() {
        state.value=BackupUiState(showExportPasswordDialog=true); render()
        val confirm=text(R.string.backup_export_confirm); confirm.assertIsNotEnabled()
        val fields=ui.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("fixture"); confirm.assertIsNotEnabled()
        fields[1].performTextReplacement("   "); confirm.assertIsNotEnabled()
        fields[1].performTextReplacement("fixture"); confirm.assertIsEnabled()
        fields[0].performTextReplacement(""); confirm.assertIsNotEnabled()
        verify(exactly=0) { vm.confirmExportPassword(any(),any()) }
    }
    @Test fun importPasswordRequiresContentAndDelegatesItsExactValue() {
        state.value=BackupUiState(showImportPasswordDialog=true); render()
        val confirm=text(R.string.backup_import_unlock); confirm.assertIsNotEnabled()
        ui.onNode(hasSetTextAction()).performTextReplacement(" \t "); confirm.assertIsNotEnabled()
        ui.onNode(hasSetTextAction()).performTextReplacement(" fixture password ")
        confirm.assertIsEnabled().performClick()
        verify(exactly=1) { vm.confirmImportPassword(" fixture password ") }
    }
    @Test fun cancellingExportDialogDoesNotStartExport() {
        state.value=BackupUiState(showExportPasswordDialog=true); render()
        text(R.string.backup_cancel).performClick()
        verify(exactly=1) { vm.dismissExportPasswordDialog() }
        verify(exactly=0) { vm.confirmExportPassword(any(),any()) }
    }
    @Test fun cancellingImportPasswordDoesNotAttemptDecryption() {
        state.value=BackupUiState(showImportPasswordDialog=true); render()
        text(R.string.backup_cancel).performClick()
        verify(exactly=1) { vm.dismissImportPasswordDialog() }
        verify(exactly=0) { vm.confirmImportPassword(any()) }
    }
    @Test fun refusingReplacementDoesNotRestoreData() {
        state.value=BackupUiState(showImportConfirmDialog=true); render()
        text(R.string.backup_confirm_no).performClick()
        verify(exactly=1) { vm.dismissImportConfirm() }
        verify(exactly=0) { vm.confirmImport() }
    }
    @Test fun exportingDisablesRepeatedExportAndImportSelection() {
        state.value=BackupUiState(isExporting=true); render()
        text(R.string.backup_export_title).performScrollTo().assertIsNotEnabled()
        text(R.string.backup_import_title).performScrollTo().assertIsNotEnabled()
        verify(exactly=0) { vm.requestExport() }
    }
    @Test fun importingDisablesExportAndRepeatedImportSelection() {
        state.value=BackupUiState(isImporting=true); render()
        text(R.string.backup_export_title).performScrollTo().assertIsNotEnabled()
        text(R.string.backup_import_title).performScrollTo().assertIsNotEnabled()
        verify(exactly=0) { vm.requestExport() }
    }
    @Test fun errorToastIsConsumedOnceAcrossUnrelatedStateChange() {
        state.value=BackupUiState(error="fixture storage error"); render(); ui.waitForIdle()
        assertEquals("fixture storage error",ShadowToast.getTextOfLatestToast())
        ui.runOnIdle { state.value=state.value.copy(lastBackupAt=1L) }; ui.waitForIdle()
        verify(exactly=1) { vm.clearError() }; verify(exactly=0) { vm.clearSuccess() }
    }
    @Test fun successToastIsConsumedOnceAcrossUnrelatedStateChange() {
        state.value=BackupUiState(successMessage="fixture restore complete"); render(); ui.waitForIdle()
        assertEquals("fixture restore complete",ShadowToast.getTextOfLatestToast())
        ui.runOnIdle { state.value=state.value.copy(lastBackupAt=1L) }; ui.waitForIdle()
        verify(exactly=1) { vm.clearSuccess() }; verify(exactly=0) { vm.clearError() }
    }
    @Test fun exportedFileShareEffectDoesNotRepeatOnUnrelatedRecomposition() {
        state.value=BackupUiState(shareFilePath="/tmp/fixture-backup.smb"); render(); ui.waitForIdle()
        ui.runOnIdle { state.value=state.value.copy(lastBackupAt=1L) }; ui.waitForIdle()
        verify(exactly=1) { vm.shareExportedFile() }
    }
}
