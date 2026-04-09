package com.trader.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * رسالة معلقة لم تُرسَل بعد (لا إنترنت).
 * تُحفظ في Room وتبقى حتى بعد إغلاق التطبيق.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey val tempId: String,
    val merchantId: String,
    val text: String,
    val senderName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFailed: Boolean = false
)
