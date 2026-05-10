package com.trader.core.domain.model

data class Session(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val loginDate: Long,
    val lastActive: Long
)
