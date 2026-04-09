package com.trader.salesmanager.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    private const val RELEASES_API =
        "https://api.github.com/repos/mohammedakkad/SalesManager/releases"
    private const val APK_ASSET_NAME = "salesmanager-release.apk"

    /**
     * Tag convention:  salesmanager-v{major}.{minor}.{patch}
     * Examples:        salesmanager-v1.0.0  →  versionCode = 10000
     *                  salesmanager-v1.0.1  →  versionCode = 10001
     *                  salesmanager-v1.1.0  →  versionCode = 10100
     *                  salesmanager-v2.0.0  →  versionCode = 20000
     *
     * Same formula used in the GitHub Actions workflow — always comparable.
     */
    suspend fun check(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                connectTimeout = 10_000
                readTimeout    = 15_000
            }

            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val releases = JSONArray(body)

            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft"))      continue
                if (release.optBoolean("prerelease")) continue

                // Find salesmanager APK asset
                val assets = release.getJSONArray("assets")
                var apkUrl = ""
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.getString("name") == APK_ASSET_NAME) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isEmpty()) continue

                // Parse "salesmanager-v1.0.1" → versionName="1.0.1" versionCode=10001
                val tagName     = release.getString("tag_name")
                val versionName = tagName.removePrefix("salesmanager-v").removePrefix("v")
                val versionCode = parseVersionCode(versionName) ?: continue

                val changelog = release.optString("body", "")
                    .split("\n")
                    .map { it.trim().removePrefix("- ").removePrefix("* ").trim() }
                    .filter { it.isNotEmpty() }

                return@withContext AppUpdateInfo(
                    latestVersion = versionCode,
                    versionName   = versionName,
                    downloadUrl   = apkUrl,
                    changelog     = changelog,
                    isForce       = true
                )
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * "1.0.1" → 10001
     * "1.1.0" → 10100
     * "2.0.0" → 20000
     * Falls back to null if format is unexpected.
     */
    private fun parseVersionCode(version: String): Int? {
        val parts = version.split(".")
        if (parts.size != 3) return null
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        return major * 10000 + minor * 100 + patch
    }
}
