package com.trader.core.data.repository

import com.trader.core.data.remote.ChatService
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(private val service: ChatService) : ChatRepository {
    override fun getMessages(merchantId: String): Flow<List<ChatMessage>> =
        service.getMessages(merchantId)

    override suspend fun sendMessage(merchantId: String, message: ChatMessage) =
        service.sendMessage(merchantId, message)

    override suspend fun markAsRead(merchantId: String, messageId: String) =
        service.markAsRead(merchantId, messageId)

    override fun getUnreadCount(merchantId: String): Flow<Int> = service.getUnreadCount(merchantId)
}
