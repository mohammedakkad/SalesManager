package com.trader.admin.ui.chat.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val editingMessage: ChatMessage? = null,
    val selectedForDelete: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val contextMessageId: String? = null
)

class ChatDetailViewModel(
    private val repo: ChatRepository,
    val merchantId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminChatUiState())
    val uiState: StateFlow<AdminChatUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _uiState.map { it.messages }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val text: StateFlow<String> = _uiState.map { it.inputText }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            repo.getMessages(merchantId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
                // ✅ Batch
                val unreadFromMerchant = msgs.filter { !it.isRead && it.senderId != SENDER_ADMIN }
                    .map { it.id }
                if (unreadFromMerchant.isNotEmpty()) {
                    repo.markAllAsRead(merchantId, unreadFromMerchant)
                }
            }
        }
    }

    fun updateText(t: String) = _uiState.update { it.copy(inputText = t) }

    // Single send() — handles both new message and edit
    fun send() {
        val editing = _uiState.value.editingMessage
        if (editing != null) saveEdit(editing) else sendNew()
    }

    private fun sendNew() {
        val t = _uiState.value.inputText.trim()
        if (t.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch {
            repo.sendMessage(merchantId,
                ChatMessage(text = t, senderId = SENDER_ADMIN, senderName = "الإدارة"))
        }
    }

    fun startEdit(msg: ChatMessage) =
        _uiState.update { it.copy(editingMessage = msg, inputText = msg.text, contextMessageId = null) }

    fun cancelEdit() = _uiState.update { it.copy(editingMessage = null, inputText = "") }

    private fun saveEdit(msg: ChatMessage) {
        val newText = _uiState.value.inputText.trim()
        if (newText.isEmpty() || newText == msg.text) { cancelEdit(); return }
        _uiState.update { it.copy(editingMessage = null, inputText = "") }
        viewModelScope.launch { repo.editMessage(merchantId, msg.id, newText) }
    }

    fun deleteMessage(id: String) {
        _uiState.update { it.copy(contextMessageId = null) }
        viewModelScope.launch { repo.deleteMessage(merchantId, id) }
    }

    fun showContext(id: String)  = _uiState.update { it.copy(contextMessageId = id) }
    fun dismissContext()         = _uiState.update { it.copy(contextMessageId = null) }

    fun enterSelectionMode(id: String) =
        _uiState.update { it.copy(isSelectionMode = true, selectedForDelete = setOf(id), contextMessageId = null) }

    fun toggleSelection(id: String) = _uiState.update {
        val set = it.selectedForDelete.toMutableSet()
        if (id in set) set.remove(id) else set.add(id)
        it.copy(selectedForDelete = set, isSelectionMode = set.isNotEmpty())
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedForDelete.toList()
        _uiState.update { it.copy(selectedForDelete = emptySet(), isSelectionMode = false) }
        viewModelScope.launch { ids.forEach { repo.deleteMessage(merchantId, it) } }
    }

    fun cancelSelection() =
        _uiState.update { it.copy(selectedForDelete = emptySet(), isSelectionMode = false) }
}
