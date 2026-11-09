package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelStore
import com.trader.core.data.local.appDataStore
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.salesmanager.R
import com.trader.salesmanager.ui.settings.backup.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupCommandsTest {
    private val manager=mockk<BackupManager>(relaxed=true);private val context=mockk<Context>(relaxed=true);private val store=ViewModelStore()
    private val prefs=MutableStateFlow<Preferences>(emptyPreferences())
    private val data=object:DataStore<Preferences> {
        override val data:Flow<Preferences> = prefs
        override suspend fun updateData(transform:suspend(Preferences)->Preferences):Preferences=transform(prefs.value).also{prefs.value=it}
    }
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher());mockkStatic("com.trader.core.data.local.DataStoreProviderKt")
        every{context.applicationContext} returns context;every{context.appDataStore} returns data
        every{context.getString(any())} answers {"resource:${firstArg<Int>()}"}
        every{context.getString(any(),*anyVararg())} answers {"resource:${firstArg<Int>()}"}
    }
    @After fun cleanup(){store.clear();unmockkStatic("com.trader.core.data.local.DataStoreProviderKt");Dispatchers.resetMain()}
    private fun model()=BackupViewModel(manager,context).also{store.put("backup",it)}
    private val payload=BackupPayload(customers=listOf(CustomerEntity(1,"fixture")))
    @Test fun exportRequiresMatchingNonblankPasswordBeforeManagerIsCalled()=runTest {
        val vm=model();vm.requestExport();assertTrue(vm.uiState.value.showExportPasswordDialog)
        vm.confirmExportPassword("","");assertEquals("resource:${R.string.backup_error_password_empty}",vm.uiState.value.error)
        vm.confirmExportPassword("fixture","different");assertEquals("resource:${R.string.backup_error_password_mismatch}",vm.uiState.value.error)
        vm.dismissExportPasswordDialog();vm.clearError();assertNull(vm.uiState.value.error);assertFalse(vm.uiState.value.showExportPasswordDialog)
        coVerify(exactly=0){manager.exportEncryptedBackup(any())}
    }
    @Test fun successfulExportClearsPasswordArrayAndPersistsTimestamp()=runTest {
        val file=File("/tmp/backup-export-fixture.smb");var password:CharArray?=null
        coEvery{manager.exportEncryptedBackup(any())} coAnswers {password=firstArg();file}
        val vm=model();vm.confirmExportPassword("fixture","fixture");runCurrent()
        assertTrue(password!!.all{it=='\u0000'});assertEquals(file.absolutePath,vm.uiState.value.shareFilePath)
        assertNotNull(vm.uiState.value.lastBackupAt);assertEquals(vm.uiState.value.lastBackupAt,prefs.value[LAST_BACKUP_AT_KEY]);assertFalse(vm.uiState.value.isExporting)
        vm.clearSuccess();assertNull(vm.uiState.value.successMessage)
        vm.shareExportedFile();assertNull(vm.uiState.value.shareFilePath);assertNotNull(vm.uiState.value.error)
    }
    @Test fun exportStorageFailureClearsSensitiveBufferAndLeavesRetryAvailable()=runTest {
        var password:CharArray?=null;coEvery{manager.exportEncryptedBackup(any())} coAnswers {password=firstArg();throw BackupException.StorageFailed}
        val vm=model();vm.confirmExportPassword("fixture","fixture");runCurrent()
        assertTrue(password!!.all{it=='\u0000'});assertFalse(vm.uiState.value.isExporting);assertEquals("resource:${R.string.backup_error_export_failed}",vm.uiState.value.error)
        assertNull(vm.uiState.value.lastBackupAt)
    }
    @Test fun encryptedImportAsksPasswordAndRequiresSeparateRestoreConfirmation()=runTest {
        val bytes=byteArrayOf(1,2);every{manager.requiresPassword(bytes)} returns true
        var secret:CharArray?=null;coEvery{manager.parseBackupFromBytes(bytes,any())} coAnswers {secret=secondArg();payload}
        val vm=model();vm.onImportBytes(bytes);assertTrue(vm.uiState.value.showImportPasswordDialog)
        vm.confirmImportPassword("");assertNotNull(vm.uiState.value.error);vm.confirmImportPassword("fixture");runCurrent()
        assertTrue(secret!!.all{it=='\u0000'});assertTrue(vm.uiState.value.showImportConfirmDialog);assertEquals(payload,vm.uiState.value.pendingImport)
        coVerify(exactly=0){manager.restorePayload(any())};vm.confirmImport();runCurrent();coVerify(exactly=1){manager.restorePayload(payload)}
        assertNull(vm.uiState.value.pendingImport);assertNotNull(vm.uiState.value.successMessage);assertFalse(vm.uiState.value.isImporting)
    }
    @Test fun cancellingEncryptedOrParsedImportDoesNotRestoreAnything()=runTest {
        val bytes=byteArrayOf(1);every{manager.requiresPassword(bytes)} returns true;val vm=model();vm.onImportBytes(bytes);vm.dismissImportPasswordDialog()
        assertNull(vm.uiState.value.pendingEncryptedImportBytes);assertFalse(vm.uiState.value.showImportPasswordDialog)
        every{manager.requiresPassword(bytes)} returns false;coEvery{manager.parseBackupFromBytes(bytes,null)} returns payload
        vm.onImportBytes(bytes);runCurrent();vm.dismissImportConfirm();vm.confirmImport();runCurrent();assertNull(vm.uiState.value.pendingImport)
        coVerify(exactly=0){manager.restorePayload(any())}
    }
    @Test fun corruptWrongPasswordEmptyAndVersionFailuresDoNotReachRestore()=runTest {
        val vm=model();val bytes=byteArrayOf(1);every{manager.requiresPassword(bytes)} returns false
        val failures=listOf(BackupException.NewerBackupVersion(999) to R.string.backup_error_newer_version,
            BackupException.UnsupportedBackupVersion(-1) to R.string.backup_error_unsupported_version,
            BackupException.WrongPasswordOrCorrupted to R.string.backup_error_wrong_password,
            BackupException.InvalidFile to R.string.backup_error_invalid_file,BackupException.Corrupted to R.string.backup_error_corrupted,
            BackupException.EmptyBackup to R.string.backup_error_empty,BackupException.ReadFailed to R.string.backup_error_read_failed,
            java.io.IOException("fixture") to R.string.backup_error_import_failed)
        for((failure,resource) in failures) {
            coEvery{manager.parseBackupFromBytes(bytes,null)} throws failure;vm.onImportBytes(bytes);runCurrent()
            assertEquals("resource:$resource",vm.uiState.value.error);assertNull(vm.uiState.value.pendingImport)
        }
        coVerify(exactly=0){manager.restorePayload(any())}
    }
    @Test fun pickerFailuresSurfaceSpecificMessagesAndConfirmWithoutPayloadIsNoOp()=runTest {
        val vm=model();vm.onImportBytes(null);assertEquals("resource:${R.string.backup_error_read_failed}",vm.uiState.value.error)
        vm.onImportFileCancelled();assertEquals("resource:${R.string.backup_error_file_cancelled}",vm.uiState.value.error)
        vm.onImportPermissionDenied();assertEquals("resource:${R.string.backup_error_permission_denied}",vm.uiState.value.error)
        vm.confirmImportPassword("unused");vm.confirmImport();runCurrent();coVerify(exactly=0){manager.restorePayload(any())}
    }
    @Test fun restoreFailureClearsPendingPayloadAndStopsProgress()=runTest {
        val bytes=byteArrayOf(1);every{manager.requiresPassword(bytes)} returns false;coEvery{manager.parseBackupFromBytes(bytes,null)} returns payload
        val vm=model();vm.onImportBytes(bytes);runCurrent();coEvery{manager.restorePayload(payload)} throws BackupException.NewerBackupVersion(999)
        vm.confirmImport();runCurrent();assertFalse(vm.uiState.value.isImporting);assertNull(vm.uiState.value.pendingImport);assertEquals("resource:${R.string.backup_error_newer_version}",vm.uiState.value.error)
    }
}
