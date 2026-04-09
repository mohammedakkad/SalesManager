package com.trader.salesmanager.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {

    private const val RELEASES_API =
        "https://api.github.com/repos/mohammedakkad/SalesManager/releases"

    // Must match the exact APK filename produced by GitHub Actions
    private const val APK_ASSET_NAME = "salesmanager-release.apk"

    /**
     * Calls GitHub Releases API and finds the latest non-draft, non-prerelease
     * that contains salesmanager-release.apk.
     *
     * Tag convention:  salesmanager-v{versionCode}
     * Examples:        salesmanager-v2  →  versionCode = 2
     *                  salesmanager-v10 →  versionCode = 10
     *
     * Release body = changelog, one entry per line, supports "- " or "* " prefixes.
     */
    suspend fun check(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
            conn.apply {
                requestMethod  = "GET"
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

                // Skip drafts and pre-releases
                if (release.optBoolean("draft")      ) continue
                if (release.optBoolean("prerelease") ) continue

                // Find the salesmanager APK asset
                val assets = release.getJSONArray("assets")
                var apkUrl = ""
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.getString("name") == APK_ASSET_NAME) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isEmpty()) continue   // This release has no salesmanager APK

                // Parse versionCode from tag name
                // "salesmanager-v2" → 2   |   "salesmanager-v10" → 10
                val tagName = release.getString("tag_name")
                val versionCode = tagName
                    .removePrefix("salesmanager-v")
                    .removePrefix("v")
                    .toIntOrNull() ?: continue

                // Release name shown to user (fallback to tag)
                val versionName = release.optString("name", tagName)
                    .ifEmpty { tagName }

                // Changelog from release body — one bullet per line
                val changelogRaw = release.optString("body", "")
                val changelog = changelogRaw
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
            null  // No matching release found
        } catch (e: Exception) {
            null  // Offline or error — never block the user
        }
    }
}
