package com.trader.core.data.repository

import com.trader.core.data.remote.ChatService
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(private val service: ChatService) : ChatRepository {
    override fun getMessages(merchantId: String)              = service.getMessages(merchantId)
    override suspend fun sendMessage(merchantId: String, message: ChatMessage) = service.sendMessage(merchantId, message)
    override suspend fun markAsRead(merchantId: String, messageId: String)     = service.markAsRead(merchantId, messageId)
    override suspend fun markAllAsRead(merchantId: String, messageIds: List<String>) = service.markAllAsRead(merchantId, messageIds)
    override suspend fun editMessage(merchantId: String, messageId: String, newText: String) = service.editMessage(merchantId, messageId, newText)
    override suspend fun deleteMessage(merchantId: String, messageId: String)  = service.deleteMessage(merchantId, messageId)
    override fun getUnreadCount(merchantId: String, excludeSenderId: String)   = service.getUnreadCount(merchantId, excludeSenderId)
}
