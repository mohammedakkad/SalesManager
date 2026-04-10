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
    object BackgroundDownloading               : UpdateUiState()  // downloading in bg, dialog closed
    object NeedInstallPermission               : UpdateUiState()
    data class DownloadError(val message: String, val canRetry: Boolean = true) : UpdateUiState()
    object UpToDate                            : UpdateUiState()
}

class AppUpdateViewModel : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var currentInfo: AppUpdateInfo? = null
    private var downloadedFile: java.io.File? = null

    /** true أثناء تحضير الرابط أو التحميل — يمنع أي ضغطة إضافية */
    private var isDownloadInProgress = false

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
        // ❶ منع الضغط المتعدد — إذا كان التحميل جارياً نتجاهل الطلب
        if (isDownloadInProgress) return

        val info = currentInfo ?: return
        if (info.downloadUrl.isEmpty()) {
            _state.value = UpdateUiState.DownloadError("رابط التحميل غير متوفر")
            return
        }

        // ❷ التحقق من الإذن أولاً (قبل تشغيل أي coroutine)
        if (!AppUpdateDownloader.canInstallUnknownSources(context)) {
            _state.value = UpdateUiState.NeedInstallPermission
            return
        }

        // ❸ قفل الزر فوراً قبل أي عملية
        isDownloadInProgress = true
        _state.value = UpdateUiState.Downloading(0)

        viewModelScope.launch {
            AppUpdateDownloader.downloadApk(context, info.downloadUrl)
                .collect { downloadState ->
                    when (downloadState) {
                        is DownloadState.Progress -> _state.value = UpdateUiState.Downloading(downloadState.percent)
                        is DownloadState.Success  -> {
                            downloadedFile = downloadState.file
                            isDownloadInProgress = false
                            _state.value = UpdateUiState.ReadyToInstall
                        }
                        is DownloadState.Error    -> {
                            isDownloadInProgress = false          // السماح بالمحاولة مجدداً عند الخطأ
                            _state.value = UpdateUiState.DownloadError(downloadState.message)
                        }
                    }
                }
        }
    }

    fun install(context: Context) {
        val file = downloadedFile ?: return
        AppUpdateDownloader.installApk(context, file)
    }

    fun retryDownload(context: Context) {
        isDownloadInProgress = false   // reset للسماح بالمحاولة الجديدة
        startDownload(context)
    }

    fun openInstallPermission(context: Context) {
        AppUpdateDownloader.openInstallPermissionSettings(context)
    }
}
