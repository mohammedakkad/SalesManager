package com.trader.core.domain.model

import com.google.firebase.Timestamp

enum class MessageStatus {
    SENDING,    // pending في الـ local (لم يُرفع بعد)
    SENT,       // وصلت لـ Firestore          ✓ رمادي
    READ        // قرأها الطرف الثاني         ✓✓ ملون
}

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val readAt: Long? = null,           // ✅ وقت القراءة
    val editedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
) {
    val status: MessageStatus
        get() = if (isRead) MessageStatus.READ else MessageStatus.SENT

    val isDeleted: Boolean get() = deletedAt != null
    val isEdited:  Boolean get() = editedAt  != null
}

const val SENDER_ADMIN = "admin"
