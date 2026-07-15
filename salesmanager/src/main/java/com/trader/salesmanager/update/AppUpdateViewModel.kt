package com.trader.salesmanager.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trader.salesmanager.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val downloadProgress: Int? = null,
    val isReadyToInstall: Boolean = false,
    val error: String? = null
)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private var downloadedFile: File? = null

    fun checkForUpdate() {
        if (_uiState.value.isChecking) return

        _uiState.update {
            it.copy(
                isChecking = true,
                error = null,
                updateAvailable = false,
                updateInfo = null,
                downloadProgress = null,
                isReadyToInstall = false
            )
        }

        viewModelScope.launch {
            val info = AppUpdateChecker.check()
            if (info == null) {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        error = "تعذر التحقق من التحديثات"
                    )
                }
                return@launch
            }

            if (info.latestVersion > BuildConfig.VERSION_CODE) {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        updateAvailable = true,
                        updateInfo = info,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        updateAvailable = false,
                        updateInfo = null,
                        error = null
                    )
                }
            }
        }
    }

    fun startDownload() {
        val info = _uiState.value.updateInfo ?: return
        if (_uiState.value.downloadProgress != null) return

        _uiState.update {
            it.copy(downloadProgress = 0, error = null, isReadyToInstall = false)
        }

        viewModelScope.launch {
            val file = AppUpdateDownloader.downloadApk(
                context = getApplication(),
                url = info.downloadUrl,
                onProgress = { percent ->
                    _uiState.update { state -> state.copy(downloadProgress = percent) }
                }
            )

            if (file != null) {
                downloadedFile = file
                saveApkPath(getApplication(), file.absolutePath)
                _uiState.update {
                    it.copy(downloadProgress = null, isReadyToInstall = true, error = null)
                }
            } else {
                _uiState.update {
                    it.copy(
                        downloadProgress = null,
                        error = "فشل تحميل التحديث"
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val context = getApplication<Application>()
        val file = downloadedFile ?: loadSavedApkFile(context)
        if (file != null) {
            AppUpdateDownloader.installApk(context, file)
        }
    }

    fun openInstallSettings() {
        AppUpdateDownloader.openInstallPermissionSettings(getApplication())
    }

    fun markReadyToInstallFromBackground() {
        val path = getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APK_PATH, null)
        if (path != null) {
            downloadedFile = File(path)
            _uiState.update {
                it.copy(
                    downloadProgress = null,
                    isReadyToInstall = true,
                    error = null
                )
            }
        }
    }

    private fun saveApkPath(context: Context, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APK_PATH, path)
            .apply()
    }

    private fun loadSavedApkFile(context: Context): File? {
        val path = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APK_PATH, null)
            ?: return null
        val file = File(path)
        return file.takeIf { it.exists() }
    }

    companion object {
        const val PREFS_NAME = "update_prefs"
        const val KEY_APK_PATH = "apk_path"
    }
}
