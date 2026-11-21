package com.trader.salesmanager.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/mohammedakkad/SalesManager/releases/latest"
    private const val APK_ASSET_NAME = "salesmanager-release.apk"
    private const val TIMEOUT_MS = 15_000

    suspend fun check(): AppUpdateInfo? = check {
        URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
    }

    /** Internal transport seam; the production endpoint and public no-argument API are unchanged. */
    internal suspend fun check(openConnection: () -> HttpURLConnection): AppUpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val conn = openConnection().also { connection = it }
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val body = conn.inputStream.bufferedReader().use { it.readText() }

            val release = JSONObject(body)
            if (release.optBoolean("draft")) return@withContext null
            if (release.optBoolean("prerelease")) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name") == APK_ASSET_NAME) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isEmpty()) return@withContext null

            val tagName = release.getString("tag_name")
            val versionName = tagName.removePrefix("salesmanager-v").removePrefix("v")
            val versionCode = parseVersionCode(versionName) ?: return@withContext null

            val changelog = release.optString("body", "")
                .split("\n")
                .map { it.trim().removePrefix("- ").removePrefix("* ").trim() }
                .filter { it.isNotEmpty() }

            AppUpdateInfo(
                latestVersion = versionCode,
                versionName = versionName,
                downloadUrl = apkUrl,
                changelog = changelog,
                isForce = false
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    private fun parseVersionCode(version: String): Int? {
        val parts = version.split(".")
        if (parts.size != 3) return null
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        // Android's version encoding reserves two decimal digits for minor and patch.
        if (major < 0 || minor !in 0..99 || patch !in 0..99) return null
        val code = major.toLong() * 10000 + minor * 100L + patch
        return code.takeIf { it <= Int.MAX_VALUE }?.toInt()
    }
}
