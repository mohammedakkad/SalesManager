package com.trader.core.domain.model

data class Session(
    val id: String,
    val deviceId: String,
    val deviceName: String,      // "Samsung Galaxy A54 (Android 14)"
    val deviceModel: String = "", // Build.MODEL منفصلاً للـ admin panel
    val loginDate: Long,
    val lastActive: Long
)
