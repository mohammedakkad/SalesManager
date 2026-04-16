package com.trader.salesmanager.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Idle                                          : UpdateUiState()
    object Checking                                      : UpdateUiState()
    data class UpdateAvailable(val info: AppUpdateInfo)  : UpdateUiState()
    data class Downloading(val percent: Int)             : UpdateUiState()
    object ReadyToInstall                                : UpdateUiState()
    object BackgroundDownloading                         : UpdateUiState()
    object NeedInstallPermission                         : UpdateUiState()
    data class DownloadError(val message: String)        : UpdateUiState()
    object UpToDate                                      : UpdateUiState()
}

class AppUpdateViewModel : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var currentInfo: AppUpdateInfo? = null
    private var downloadedFile: java.io.File? = null
    private var isDownloadInProgress = false

    fun checkForUpdate(currentVersionCode: Int) {
        if (_state.value is UpdateUiState.Checking) return
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val info = AppUpdateChecker.check()
            if (info == null || !info.isUpdateAvailable) {
                _state.value = UpdateUiState.UpToDate
                return@launch
            }
            if (info.latestVersion > currentVersionCode) {
                currentInfo = info
                // ← لا نُظهر Dialog إجبارياً — فقط نُعلم بوجود تحديث
                _state.value = UpdateUiState.UpdateAvailable(info)
            } else {
                _state.value = UpdateUiState.UpToDate
            }
        }
    }

    /**
     * التحميل في الخلفية عبر WorkManager — يظهر في شريط الإشعارات
     * ويبقى التطبيق قابلاً للاستخدام بشكل طبيعي.
     */
    fun startBackgroundDownload(context: Context) {
        val info = currentInfo ?: return
        if (info.downloadUrl.isEmpty()) return
        BackgroundUpdateWorker.schedule(context, info.downloadUrl, info.versionName)
        _state.value = UpdateUiState.BackgroundDownloading
    }

    /**
     * تحميل مباشر (عندما يضغط المستخدم "تحميل الآن" من الإعدادات)
     */
    fun startDirectDownload(context: Context) {
        if (isDownloadInProgress) return
        val info = currentInfo ?: return
        if (!AppUpdateDownloader.canInstallUnknownSources(context)) {
            _state.value = UpdateUiState.NeedInstallPermission
            return
        }
        isDownloadInProgress = true
        _state.value = UpdateUiState.Downloading(0)
        viewModelScope.launch {
            AppUpdateDownloader.downloadApk(context, info.downloadUrl).collect { ds ->
                when (ds) {
                    is DownloadState.Progress -> _state.value = UpdateUiState.Downloading(ds.percent)
                    is DownloadState.Success  -> {
                        downloadedFile = ds.file
                        isDownloadInProgress = false
                        _state.value = UpdateUiState.ReadyToInstall
                    }
                    is DownloadState.Error -> {
                        isDownloadInProgress = false
                        _state.value = UpdateUiState.DownloadError(ds.message)
                    }
                }
            }
        }
    }

    // للتوافق مع الكود القديم
    fun startDownload(context: Context) = startBackgroundDownload(context)

    fun install(context: Context) {
        val file = downloadedFile
        if (file != null) {
            AppUpdateDownloader.installApk(context, file)
            return
        }
        // التحقق من APK المحفوظ من WorkManager
        val path = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
            .getString("apk_path", null)
        if (path != null) {
            AppUpdateDownloader.installApk(context, java.io.File(path))
        }
    }

    fun retryDownload(context: Context) {
        isDownloadInProgress = false
        startDirectDownload(context)
    }

    fun openInstallPermission(context: Context) {
        AppUpdateDownloader.openInstallPermissionSettings(context)
    }

    fun dismiss() {
        _state.value = UpdateUiState.Idle
    }
}
