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
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val locallyDeletedIds: Set<String> = emptySet(),
    val pendingOwnDeleteIds: Set<String> = emptySet(),
    val showDeleteDialog: Boolean = false
) {
    val visibleMessages: List<ChatMessage>
    get() = messages.filter {
        it.id !in locallyDeletedIds
    }

    val selectedMessages: List<ChatMessage>
    get() = messages.filter {
        it.id in selectedIds
    }

    // الأدمن يملك جميع رسائله بـ SENDER_ADMIN
    val canEdit: Boolean
    get() = selectedIds.size == 1 &&
    selectedMessages.firstOrNull()
    ?.let {
        it.senderId == SENDER_ADMIN && !it.isDeleted
    } == true

    val canCopy: Boolean
    get() = selectedMessages.any {
        !it.isDeleted
    }

    val anySelectedOwn: Boolean
    get() = selectedMessages.any {
        it.senderId == SENDER_ADMIN
    }
}

class ChatDetailViewModel(
    private val repo: ChatRepository,
    val merchantId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminChatUiState())
    val uiState: StateFlow<AdminChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getMessages(merchantId).collect {
                msgs ->
                _uiState.update {
                    it.copy(messages = msgs)
                }
                msgs.filter {
                    !it.isRead && it.senderId != SENDER_ADMIN
                }
                .forEach {
                    repo.markAsRead(merchantId, it.id)
                }
            }
        }
    }

    fun updateText(t: String) = _uiState.update {
        it.copy(inputText = t)
    }

    fun send() {
        val editing = _uiState.value.editingMessage
        if (editing != null) saveEdit(editing) else sendNew()
    }

    private fun sendNew() {
        val t = _uiState.value.inputText.trim()
        if (t.isEmpty()) return
        _uiState.update {
            it.copy(inputText = "")
        }
        viewModelScope.launch {
            repo.sendMessage(merchantId, ChatMessage(text = t, senderId = SENDER_ADMIN, senderName = "الإدارة"))
        }
    }

    fun cancelEdit() = _uiState.update {
        it.copy(editingMessage = null, inputText = "")
    }

    private fun saveEdit(msg: ChatMessage) {
        val newText = _uiState.value.inputText.trim()
        if (newText.isEmpty() || newText == msg.text) {
            cancelEdit(); return
        }
        _uiState.update {
            it.copy(editingMessage = null, inputText = "", isSelectionMode = false, selectedIds = emptySet())
        }
        viewModelScope.launch {
            repo.editMessage(merchantId, msg.id, newText)
        }
    }

    // ── وضع التحديد ──────────────────────────────────────────────

    fun onLongPress(msg: ChatMessage) {
        if (_uiState.value.isSelectionMode) {
            toggleSelection(msg.id)
        } else {
            _uiState.update {
                it.copy(isSelectionMode = true, selectedIds = setOf(msg.id))
            }
        }
    }

    fun onTap(msg: ChatMessage) {
        if (_uiState.value.isSelectionMode) toggleSelection(msg.id)
    }

    fun toggleSelection(id: String) = _uiState.update {
        val set = it.selectedIds.toMutableSet()
        if (id in set) set.remove(id) else set.add(id)
        it.copy(selectedIds = set, isSelectionMode = set.isNotEmpty())
    }

    fun cancelSelection() = _uiState.update {
        it.copy(selectedIds = emptySet(), isSelectionMode = false)
    }

    // ── تعديل ────────────────────────────────────────────────────

    fun startEditSelected() {
        val msg = _uiState.value.selectedMessages.firstOrNull() ?: return
        if (msg.senderId != SENDER_ADMIN) return
        _uiState.update {
            it.copy(editingMessage = msg, inputText = msg.text, selectedIds = emptySet(), isSelectionMode = false)
        }
    }

    // ── نسخ ──────────────────────────────────────────────────────

    fun buildCopyText(): String =
    _uiState.value.selectedMessages
    .filter {
        !it.isDeleted
    }
    .sortedBy {
        it.timestamp?.seconds ?: 0L
    }
    .joinToString("\n") {
        it.text
    }

    // ── حذف ذكي ──────────────────────────────────────────────────

    fun requestDeleteSelected() {
        val state = _uiState.value
        val selected = state.selectedMessages
        val othersIds = selected.filter {
            it.senderId != SENDER_ADMIN
        }.map {
            it.id
        }.toSet()
        val ownIds = selected.filter {
            it.senderId == SENDER_ADMIN
        }.map {
            it.id
        }.toSet()

        if (othersIds.isNotEmpty()) {
            _uiState.update {
                it.copy(locallyDeletedIds = it.locallyDeletedIds + othersIds)
            }
        }
        if (ownIds.isNotEmpty()) {
            _uiState.update {
                it.copy(pendingOwnDeleteIds = ownIds, showDeleteDialog = true,
                    selectedIds = emptySet(), isSelectionMode = false)
            }
        } else {
            _uiState.update {
                it.copy(selectedIds = emptySet(), isSelectionMode = false)
            }
        }
    }

    fun confirmDeleteForMe() {
        val ids = _uiState.value.pendingOwnDeleteIds
        _uiState.update {
            it.copy(locallyDeletedIds = it.locallyDeletedIds + ids,
                pendingOwnDeleteIds = emptySet(), showDeleteDialog = false)
        }
    }

    fun confirmDeleteForEveryone() {
        val ids = _uiState.value.pendingOwnDeleteIds
        _uiState.update {
            it.copy(locallyDeletedIds = it.locallyDeletedIds + ids,
                pendingOwnDeleteIds = emptySet(), showDeleteDialog = false)
        }
        viewModelScope.launch {
            ids.forEach {
                id ->
                try {
                    repo.deleteMessage(merchantId, id)
                    _uiState.update {
                        it.copy(locallyDeletedIds = it.locallyDeletedIds - id)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun dismissDeleteDialog() = _uiState.update {
        it.copy(showDeleteDialog = false, pendingOwnDeleteIds = emptySet())
    }
}