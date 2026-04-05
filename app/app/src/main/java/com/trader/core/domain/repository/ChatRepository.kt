package com.trader.core.domain.repository

import com.trader.core.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(merchantId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(merchantId: String, message: ChatMessage)
    suspend fun markAsRead(merchantId: String, messageId: String)
    fun getUnreadCount(merchantId: String): Flow<Int>
}
