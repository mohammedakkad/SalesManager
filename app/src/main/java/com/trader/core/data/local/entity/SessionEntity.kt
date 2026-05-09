package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trader.core.domain.model.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val deviceName: String,
    val loginDate: Long,
    val lastActive: Long
) {
    fun toDomain() = Session(
        id = id,
        deviceId = deviceId,
        deviceName = deviceName,
        loginDate = loginDate,
        lastActive = lastActive
    )
}

fun Session.toEntity() = SessionEntity(
    id = id,
    deviceId = deviceId,
    deviceName = deviceName,
    loginDate = loginDate,
    lastActive = lastActive
)
