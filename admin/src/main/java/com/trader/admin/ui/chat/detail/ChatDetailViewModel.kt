package com.trader.admin.ui.chat.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val repo: ChatRepository,
    val merchantId: String
) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> = repo.getMessages(merchantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    fun updateText(t: String) { _text.value = t }

    fun send() {
        val t = _text.value.trim()
        if (t.isEmpty()) return
        _text.value = ""
        viewModelScope.launch {
            repo.sendMessage(merchantId, ChatMessage(text = t, senderId = SENDER_ADMIN, senderName = "الإدارة"))
        }
    }
}
