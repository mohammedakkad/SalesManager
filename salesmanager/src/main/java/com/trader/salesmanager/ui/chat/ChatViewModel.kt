package com.trader.salesmanager.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMessage(
    val tempId: String,
    val text: String,
    val isFailed: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingMessages: List<PendingMessage> = emptyList(),
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
                    msgs.filter { !it.isRead && it.senderId == SENDER_ADMIN }
                        .forEach { chatRepo.markAsRead(id, it.id) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val id   = _uiState.value.merchantId
        if (text.isEmpty() || id.isEmpty()) return

        val tempId = System.currentTimeMillis().toString()
        _uiState.update {
            it.copy(
                inputText       = "",
                pendingMessages = it.pendingMessages + PendingMessage(tempId, text)
            )
        }
        viewModelScope.launch {
            try {
                chatRepo.sendMessage(id, ChatMessage(text = text, senderId = id, senderName = "تاجر"))
                // Remove from pending on success (Firestore listener will add it to messages)
                _uiState.update { s ->
                    s.copy(pendingMessages = s.pendingMessages.filter { it.tempId != tempId })
                }
            } catch (e: Exception) {
                // Mark as failed — show retry button
                _uiState.update { s ->
                    s.copy(pendingMessages = s.pendingMessages.map {
                        if (it.tempId == tempId) it.copy(isFailed = true) else it
                    })
                }
            }
        }
    }

    fun retryMessage(tempId: String) {
        val msg = _uiState.value.pendingMessages.find { it.tempId == tempId } ?: return
        val id  = _uiState.value.merchantId
        _uiState.update { s ->
            s.copy(pendingMessages = s.pendingMessages.map {
                if (it.tempId == tempId) it.copy(isFailed = false) else it
            })
        }
        viewModelScope.launch {
            try {
                chatRepo.sendMessage(id, ChatMessage(text = msg.text, senderId = id, senderName = "تاجر"))
                _uiState.update { s ->
                    s.copy(pendingMessages = s.pendingMessages.filter { it.tempId != tempId })
                }
            } catch (e: Exception) {
                _uiState.update { s ->
                    s.copy(pendingMessages = s.pendingMessages.map {
                        if (it.tempId == tempId) it.copy(isFailed = true) else it
                    })
                }
            }
        }
    }

    fun dismissFailedMessage(tempId: String) {
        _uiState.update { s ->
            s.copy(pendingMessages = s.pendingMessages.filter { it.tempId != tempId })
        }
    }
}