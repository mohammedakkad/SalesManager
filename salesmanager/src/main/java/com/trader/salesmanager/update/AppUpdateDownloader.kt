package com.trader.salesmanager.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data class Progress(val percent: Int) : DownloadState()
    data class Success(val file: File)    : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object AppUpdateDownloader {

    fun downloadApk(context: Context, url: String): Flow<DownloadState> = flow {
        try {
            val apkFile = File(context.getExternalFilesDir(null), "update.apk")
            if (apkFile.exists()) apkFile.delete()

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            val totalBytes = connection.contentLength
            val input  = connection.inputStream.buffered()
            val output = apkFile.outputStream().buffered()

            var downloaded = 0L
            val buffer = ByteArray(8192)
            var lastEmittedPercent = -1

            while (true) {
                val bytes = input.read(buffer)
                if (bytes == -1) break
                output.write(buffer, 0, bytes)
                downloaded += bytes
                if (totalBytes > 0) {
                    val percent = ((downloaded * 100) / totalBytes).toInt()
                    if (percent != lastEmittedPercent) {
                        emit(DownloadState.Progress(percent))
                        lastEmittedPercent = percent
                    }
                }
            }
            output.flush()
            output.close()
            input.close()

            emit(DownloadState.Success(apkFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "خطأ في التحميل"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(context: Context, apkFile: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun canInstallUnknownSources(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
