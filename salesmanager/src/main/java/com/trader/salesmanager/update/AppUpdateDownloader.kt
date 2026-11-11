package com.trader.salesmanager.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateDownloader {

    private const val APK_FILE_NAME = "salesmanager-update.apk"

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var apkFile: File? = null
        var completed = false
        try {
            val updateDir = File(context.cacheDir, "update").apply { mkdirs() }
            val destination = File(updateDir, APK_FILE_NAME)
            apkFile = destination
            if (destination.exists()) check(destination.delete()) { "Cannot replace previous download" }

            val activeConnection = URL(url).openConnection() as HttpURLConnection
            connection = activeConnection
            activeConnection.connectTimeout = 15_000
            activeConnection.readTimeout = 15_000
            activeConnection.connect()
            val totalBytes = activeConnection.contentLength
            var downloaded = 0L
            activeConnection.inputStream.buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val bytes = input.read(buffer)
                        if (bytes == -1) break
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        val percent = if (totalBytes > 0) {
                            ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else 0
                        onProgress(percent)
                    }
                }
            }
            if (totalBytes >= 0 && downloaded != totalBytes.toLong()) {
                throw java.io.IOException("Incomplete update download")
            }
            currentCoroutineContext().ensureActive()
            onProgress(100)
            currentCoroutineContext().ensureActive()
            completed = true
            destination
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
            if (!completed) runCatching { apkFile?.delete() }
        }
    }

    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallUnknownApps(context)) {
            openInstallPermissionSettings(context)
            return
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun canInstallUnknownApps(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
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
