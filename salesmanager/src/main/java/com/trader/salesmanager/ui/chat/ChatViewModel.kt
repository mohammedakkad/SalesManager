package com.trader.salesmanager.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val merchantId: String = ""
)

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { loadChat() }

    private fun loadChat() {
        viewModelScope.launch {
            val id = activationRepo.getMerchantCode()
            _uiState.update { it.copy(merchantId = id) }
            chatRepo.getMessages(id)
                .onEach { msgs ->
                    _uiState.update { it.copy(messages = msgs, isLoading = false) }
                    // Mark admin messages as read
                    msgs.filter { !it.isRead && it.senderId == SENDER_ADMIN }
                        .forEach { chatRepo.markAsRead(id, it.id) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            val id = _uiState.value.merchantId
            chatRepo.sendMessage(id, ChatMessage(
                text       = text,
                senderId   = id,
                senderName = "تاجر"
            ))
        }
    }
}
