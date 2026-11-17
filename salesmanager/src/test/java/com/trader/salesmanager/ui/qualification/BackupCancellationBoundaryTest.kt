package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelStore
import com.trader.core.data.local.appDataStore
import com.trader.core.data.local.entity.CustomerEntity
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

/** Public commands with controlled scheduling; no reflection or production network. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupCancellationBoundaryTest {
    private val manager=mockk<BackupManager>(relaxed=true)
    private val context=mockk<Context>(relaxed=true)
    private val store=ViewModelStore()
    private val prefs=MutableStateFlow<Preferences>(emptyPreferences())
    private val data=object:DataStore<Preferences> {
        override val data:Flow<Preferences> = prefs
        override suspend fun updateData(transform:suspend(Preferences)->Preferences)=transform(prefs.value).also{prefs.value=it}
    }
    private val bytes=byteArrayOf(1,2,3)
    private val payload=BackupPayload(customers=listOf(CustomerEntity(1,"fixture")))
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic("com.trader.core.data.local.DataStoreProviderKt")
        every{context.applicationContext} returns context
        every{context.appDataStore} returns data
        every{context.getString(any())} answers {"resource:${firstArg<Int>()}"}
        every{context.getString(any(),*anyVararg())} answers {"resource:${firstArg<Int>()}"}
        every{manager.requiresPassword(bytes)} returns false
        coEvery{manager.parseBackupFromBytes(bytes,null)} returns payload
    }
    @After fun cleanup() {
        store.clear();unmockkStatic("com.trader.core.data.local.DataStoreProviderKt");Dispatchers.resetMain()
    }
    private fun model()=BackupViewModel(manager,context).also{store.put("backup",it)}

    @Test fun exportCancellationCancelsAttemptClearsPasswordAndDoesNotPublishFailure()=runTest {
        var attempt:Job?=null;var secret:CharArray?=null
        coEvery{manager.exportEncryptedBackup(any())} coAnswers {
            attempt=currentCoroutineContext()[Job];secret=firstArg();throw CancellationException("fixture cancellation")
        }
        val vm=model();vm.confirmExportPassword("fixture","fixture");runCurrent()
        assertTrue("Cancellation must remain cancellation",attempt!!.isCancelled)
        assertTrue(secret!!.all{it=='\u0000'})
        assertFalse(vm.uiState.value.isExporting);assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.shareFilePath);assertNull(prefs.value[LAST_BACKUP_AT_KEY])
        assertNull(vm.uiState.value.successMessage)
    }
    @Test fun plainImportCancellationDoesNotOfferRestoreOrPublishFailure()=runTest {
        var attempt:Job?=null
        coEvery{manager.parseBackupFromBytes(bytes,null)} coAnswers {
            attempt=currentCoroutineContext()[Job];throw CancellationException("fixture cancellation")
        }
        val vm=model();vm.onImportBytes(bytes);runCurrent()
        assertTrue(attempt!!.isCancelled);assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.pendingImport);assertFalse(vm.uiState.value.showImportConfirmDialog)
        coVerify(exactly=0){manager.restorePayload(any())}
    }
    @Test fun encryptedImportCancellationClearsPasswordWithoutFalseFailure()=runTest {
        every{manager.requiresPassword(bytes)} returns true
        var attempt:Job?=null;var secret:CharArray?=null
        coEvery{manager.parseBackupFromBytes(bytes,any())} coAnswers {
            attempt=currentCoroutineContext()[Job];secret=secondArg();throw CancellationException("fixture cancellation")
        }
        val vm=model();vm.onImportBytes(bytes);vm.confirmImportPassword("fixture");runCurrent()
        assertTrue(attempt!!.isCancelled);assertTrue(secret!!.all{it=='\u0000'})
        assertNull(vm.uiState.value.error);assertNull(vm.uiState.value.pendingImport)
        assertNull(vm.uiState.value.pendingEncryptedImportBytes)
        assertFalse(vm.uiState.value.showImportConfirmDialog)
    }
    @Test fun restoreCancellationKeepsPreparedPayloadWithoutFalseFailure()=runTest {
        var attempt:Job?=null
        coEvery{manager.restorePayload(payload)} coAnswers {
            attempt=currentCoroutineContext()[Job];throw CancellationException("fixture cancellation")
        }
        val vm=model();vm.onImportBytes(bytes);runCurrent();vm.confirmImport();runCurrent()
        assertTrue(attempt!!.isCancelled);assertFalse(vm.uiState.value.isImporting)
        assertNull(vm.uiState.value.error);assertNull(vm.uiState.value.successMessage)
        assertEquals(payload,vm.uiState.value.pendingImport)
    }
    @Test fun queuedExportConfirmationsReserveOneAttemptBeforeDispatch()=runTest {
        coEvery{manager.exportEncryptedBackup(any())} returns File("/tmp/unused-backup-fixture.smbk")
        val vm=model();vm.confirmExportPassword("fixture","fixture");vm.confirmExportPassword("fixture","fixture");runCurrent()
        coVerify(exactly=1){manager.exportEncryptedBackup(any())}
        assertFalse(vm.uiState.value.isExporting)
    }
    @Test fun queuedRestoreConfirmationsReserveOneAttemptBeforeDispatch()=runTest {
        val vm=model();vm.onImportBytes(bytes);runCurrent()
        vm.confirmImport();vm.confirmImport();runCurrent()
        coVerify(exactly=1){manager.restorePayload(payload)}
        assertFalse(vm.uiState.value.isImporting);assertNull(vm.uiState.value.pendingImport)
    }
}
