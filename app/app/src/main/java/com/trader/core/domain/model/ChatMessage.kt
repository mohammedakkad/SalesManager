package com.trader.core.domain.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)

const val SENDER_ADMIN = "admin"
