package com.trader.salesmanager.update

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object AppUpdateChecker {

    /**
     * Firebase Realtime DB structure:
     * app_updates/salesmanager/
     *   latest_version: 2         (Int — versionCode)
     *   version_name: "1.1.0"
     *   download_url: "https://firebasestorage.googleapis.com/..."
     *   is_force: true
     *   changelog: ["ميزة جديدة: تقارير تفصيلية", "إصلاح: مشكلة المزامنة", "تحسين: سرعة التطبيق"]
     */
    suspend fun check(): AppUpdateInfo? {
        return try {
            val snap = FirebaseDatabase.getInstance()
                .reference
                .child("app_updates")
                .child("salesmanager")
                .get()
                .await()

            if (!snap.exists()) return null

            val latestVersion = (snap.child("latest_version").getValue(Long::class.java) ?: 0L).toInt()
            val versionName   = snap.child("version_name").getValue(String::class.java) ?: ""
            val downloadUrl   = snap.child("download_url").getValue(String::class.java) ?: ""
            val isForce       = snap.child("is_force").getValue(Boolean::class.java) ?: true

            val changelog = mutableListOf<String>()
            snap.child("changelog").children.forEach { child ->
                child.getValue(String::class.java)?.let { changelog.add(it) }
            }

            AppUpdateInfo(
                latestVersion = latestVersion,
                versionName   = versionName,
                downloadUrl   = downloadUrl,
                changelog     = changelog,
                isForce       = isForce
            )
        } catch (e: Exception) {
            null // offline or error — don't block
        }
    }
}
