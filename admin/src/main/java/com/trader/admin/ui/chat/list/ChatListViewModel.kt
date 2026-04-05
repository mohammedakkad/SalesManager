package com.trader.admin.ui.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.Merchant
import com.trader.core.domain.repository.ChatRepository
import com.trader.core.domain.repository.MerchantAdminRepository
import kotlinx.coroutines.flow.*

data class ChatListItem(val merchant: Merchant, val unreadCount: Int)

class ChatListViewModel(
    private val merchantRepo: MerchantAdminRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {
    val chats: StateFlow<List<ChatListItem>> = merchantRepo.getAllMerchants()
        .map { merchants ->
            merchants.map { m ->
                val unread = chatRepo.getUnreadCount(m.id).firstOrNull() ?: 0
                ChatListItem(m, unread)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
