package com.trader.core.domain.model

import com.google.firebase.Timestamp

enum class MessageStatus {
    SENDING,   // pending في الـ local
    SENT,      // وصلت لـ Firestore  (✓)
    READ       // قرأها الطرف الثاني (✓✓)
}

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val editedAt: Timestamp? = null,   // null = لم يُعدَّل
    val deletedAt: Timestamp? = null   // null = لم يُحذَف (soft delete)
) {
    val status: MessageStatus
        get() = if (isRead) MessageStatus.READ else MessageStatus.SENT

    val isDeleted: Boolean get() = deletedAt != null
    val isEdited:  Boolean get() = editedAt  != null
}

const val SENDER_ADMIN = "admin"
