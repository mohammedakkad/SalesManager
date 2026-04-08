package com.trader.salesmanager.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Idle                                : UpdateUiState()
    object Checking                            : UpdateUiState()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateUiState()
    data class Downloading(val percent: Int)   : UpdateUiState()
    object ReadyToInstall                      : UpdateUiState()
    object NeedInstallPermission               : UpdateUiState()
    data class DownloadError(val message: String, val canRetry: Boolean = true) : UpdateUiState()
    object UpToDate                            : UpdateUiState()
}

class AppUpdateViewModel : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var currentInfo: AppUpdateInfo? = null
    private var downloadedFile: java.io.File? = null

    fun checkForUpdate(currentVersionCode: Int) {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val info = AppUpdateChecker.check()
            if (info == null || !info.isUpdateAvailable) {
                _state.value = UpdateUiState.UpToDate
                return@launch
            }
            if (info.latestVersion > currentVersionCode) {
                currentInfo = info
                _state.value = UpdateUiState.UpdateAvailable(info)
            } else {
                _state.value = UpdateUiState.UpToDate
            }
        }
    }

    fun startDownload(context: Context) {
        val info = currentInfo ?: return
        if (info.downloadUrl.isEmpty()) {
            _state.value = UpdateUiState.DownloadError("رابط التحميل غير متوفر")
            return
        }

        // Check install permission first
        if (!AppUpdateDownloader.canInstallUnknownSources(context)) {
            _state.value = UpdateUiState.NeedInstallPermission
            return
        }

        viewModelScope.launch {
            AppUpdateDownloader.downloadApk(context, info.downloadUrl)
                .collect { downloadState ->
                    when (downloadState) {
                        is DownloadState.Progress -> _state.value = UpdateUiState.Downloading(downloadState.percent)
                        is DownloadState.Success  -> {
                            downloadedFile = downloadState.file
                            _state.value = UpdateUiState.ReadyToInstall
                        }
                        is DownloadState.Error    -> _state.value = UpdateUiState.DownloadError(downloadState.message)
                    }
                }
        }
    }

    fun install(context: Context) {
        val file = downloadedFile ?: return
        AppUpdateDownloader.installApk(context, file)
    }

    fun retryDownload(context: Context) {
        startDownload(context)
    }

    fun openInstallPermission(context: Context) {
        AppUpdateDownloader.openInstallPermissionSettings(context)
    }
}
