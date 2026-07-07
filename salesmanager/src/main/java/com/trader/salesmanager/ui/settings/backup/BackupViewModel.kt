package com.trader.salesmanager.ui.settings.backup

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.local.appDataStore
import com.trader.salesmanager.R
import com.trader.salesmanager.util.export.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

val LAST_BACKUP_AT_KEY = longPreferencesKey("last_backup_at")

data class BackupUiState(
    val lastBackupAt: Long? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val showImportConfirmDialog: Boolean = false,
    val pendingImport: BackupPayload? = null,
    val shareFilePath: String? = null,
    val successMessage: String? = null,
    val error: String? = null
)

class BackupViewModel(
    private val backupManager: BackupManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.appDataStore.data
                .map { prefs -> prefs[LAST_BACKUP_AT_KEY] }
                .collect { timestamp ->
                    _uiState.update { it.copy(lastBackupAt = timestamp) }
                }
        }
    }

    fun exportBackup() {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null, successMessage = null) }
            try {
                val file = backupManager.exportToZip()
                val now = System.currentTimeMillis()
                context.appDataStore.edit { prefs -> prefs[LAST_BACKUP_AT_KEY] = now }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastBackupAt = now,
                        shareFilePath = file.absolutePath,
                        successMessage = context.getString(R.string.backup_export_success)
                    )
                }
            } catch (e: Exception) {
                Log.e(BACKUP_LOG_TAG, "Export failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = context.getString(R.string.backup_error_export_failed)
                    )
                }
            }
        }
    }

    fun onImportBytes(bytes: ByteArray?) {
        if (bytes == null) {
            _uiState.update {
                it.copy(error = context.getString(R.string.backup_error_read_failed))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, successMessage = null) }
            try {
                val payload = backupManager.parseBackupFromBytes(bytes)
                backupManager.validatePayload(payload)
                _uiState.update {
                    it.copy(
                        pendingImport = payload,
                        showImportConfirmDialog = true
                    )
                }
            } catch (e: BackupException.IncompatibleSchema) {
                Log.e(
                    BACKUP_LOG_TAG,
                    "Import rejected: incompatible schema ${e.found}, expected $BACKUP_SCHEMA_VERSION"
                )
                _uiState.update {
                    it.copy(
                        error = context.getString(
                            R.string.backup_error_incompatible_schema,
                            e.found,
                            BACKUP_SCHEMA_VERSION
                        )
                    )
                }
            } catch (_: BackupException.InvalidFile) {
                Log.e(BACKUP_LOG_TAG, "Import rejected: picked file is not a valid backup archive")
                _uiState.update {
                    it.copy(error = context.getString(R.string.backup_error_invalid_file))
                }
            } catch (_: BackupException.Corrupted) {
                Log.e(BACKUP_LOG_TAG, "Import rejected: backup file is corrupted or unreadable")
                _uiState.update {
                    it.copy(error = context.getString(R.string.backup_error_corrupted))
                }
            } catch (_: BackupException.EmptyBackup) {
                Log.e(BACKUP_LOG_TAG, "Import rejected: backup payload contains no merchant data")
                _uiState.update {
                    it.copy(error = context.getString(R.string.backup_error_empty))
                }
            } catch (_: BackupException.ReadFailed) {
                Log.e(BACKUP_LOG_TAG, "Import rejected: could not read picked file bytes")
                _uiState.update {
                    it.copy(error = context.getString(R.string.backup_error_read_failed))
                }
            } catch (e: Exception) {
                Log.e(
                    BACKUP_LOG_TAG,
                    "Import parse failed: ${e.javaClass.simpleName}: ${e.message}",
                    e
                )
                _uiState.update {
                    it.copy(error = context.getString(R.string.backup_error_import_failed))
                }
            }
        }
    }

    fun onImportFileCancelled() {
        _uiState.update {
            it.copy(error = context.getString(R.string.backup_error_file_cancelled))
        }
    }

    fun onImportPermissionDenied() {
        Log.e(BACKUP_LOG_TAG, "Import rejected: URI read permission denied")
        _uiState.update {
            it.copy(error = context.getString(R.string.backup_error_permission_denied))
        }
    }

    fun dismissImportConfirm() {
        _uiState.update {
            it.copy(showImportConfirmDialog = false, pendingImport = null)
        }
    }

    fun confirmImport() {
        val payload = _uiState.value.pendingImport ?: return
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    showImportConfirmDialog = false,
                    error = null,
                    successMessage = null
                )
            }
            try {
                backupManager.restorePayload(payload)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingImport = null,
                        successMessage = context.getString(R.string.backup_import_success)
                    )
                }
            } catch (e: BackupException.IncompatibleSchema) {
                Log.e(
                    BACKUP_LOG_TAG,
                    "Restore rejected: incompatible schema ${e.found}, expected $BACKUP_SCHEMA_VERSION"
                )
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingImport = null,
                        error = context.getString(
                            R.string.backup_error_incompatible_schema,
                            e.found,
                            BACKUP_SCHEMA_VERSION
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    BACKUP_LOG_TAG,
                    "Restore failed: ${e.javaClass.simpleName}: ${e.message}",
                    e
                )
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingImport = null,
                        error = context.getString(R.string.backup_error_import_failed)
                    )
                }
            }
        }
    }

    fun shareExportedFile() {
        val path = _uiState.value.shareFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            _uiState.update {
                it.copy(
                    shareFilePath = null,
                    error = context.getString(R.string.backup_error_export_failed)
                )
            }
            return
        }
        ExportManager.shareFile(context, file, "application/zip")
        _uiState.update { it.copy(shareFilePath = null) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
}
