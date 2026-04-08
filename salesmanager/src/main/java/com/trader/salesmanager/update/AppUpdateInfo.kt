package com.trader.salesmanager.update

data class AppUpdateInfo(
    val latestVersion: Int = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val changelog: List<String> = emptyList(),
    val isForce: Boolean = true
) {
    val isUpdateAvailable: Boolean get() = latestVersion > 0
}
