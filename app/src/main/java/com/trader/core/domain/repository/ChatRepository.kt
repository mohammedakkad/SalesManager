package com.trader.core.domain.repository

import com.trader.core.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(merchantId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(merchantId: String, message: ChatMessage)
    suspend fun markAsRead(merchantId: String, messageId: String)
    // ✅ Batch — أكفأ من استدعاء markAsRead في forEach
    suspend fun markAllAsRead(merchantId: String, messageIds: List<String>)
    suspend fun editMessage(merchantId: String, messageId: String, newText: String)
    suspend fun deleteMessage(merchantId: String, messageId: String)
    fun getUnreadCount(merchantId: String, excludeSenderId: String): Flow<Int>
}
