package com.trader.salesmanager.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository
) : ViewModel() {

    private val _merchantId = MutableStateFlow("")
    val messages: StateFlow<List<ChatMessage>> = _merchantId
        .filter { it.isNotEmpty() }
        .flatMapLatest { chatRepo.getMessages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        viewModelScope.launch {
            _merchantId.value = activationRepo.getMerchantCode()
        }
    }

    fun updateText(t: String) { _text.value = t }

    fun send() {
        val t = _text.value.trim()
        val id = _merchantId.value
        if (t.isEmpty() || id.isEmpty()) return
        _text.value = ""
        viewModelScope.launch {
            _isSending.value = true
            chatRepo.sendMessage(id, ChatMessage(
                text       = t,
                senderId   = id,
                senderName = "البائع"
            ))
            _isSending.value = false
        }
    }
}