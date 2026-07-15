package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Session

@Entity(tableName = "sessions",
    indices = [
        Index(value = ["lastActive"])
    ])
data class SessionEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val deviceName: String,
    val deviceModel: String = "",  // ✅ جديد — Migration 15→16 يضيفه
    val loginDate: Long,
    val lastActive: Long
) {
    fun toDomain() = Session(
        id = id,
        deviceId = deviceId,
        deviceName = deviceName,
        deviceModel = deviceModel,
        loginDate = loginDate,
        lastActive = lastActive
    )
}

fun Session.toEntity() = SessionEntity(
    id = id,
    deviceId = deviceId,
    deviceName = deviceName,
    deviceModel = deviceModel,
    loginDate = loginDate,
    lastActive = lastActive
)
