package com.trader.salesmanager.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trader.core.data.local.dao.PendingMessageDao
import com.trader.core.data.local.entity.PendingMessageEntity
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.domain.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingMessages: List<PendingMessageEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val merchantId: String = "",
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

    val canEdit: Boolean
    get() = selectedIds.size == 1 &&
    selectedMessages.firstOrNull()
    ?.let {
        it.senderId == merchantId && !it.isDeleted
    } == true

    val canCopy: Boolean
    get() = selectedMessages.any {
        !it.isDeleted
    }

    val anySelectedOwn: Boolean
    get() = selectedMessages.any {
        it.senderId == merchantId
    }
}

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository,
    private val pendingDao: PendingMessageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            val id = activationRepo.getMerchantCode()
            _uiState.update {
                it.copy(merchantId = id)
            }
            chatRepo.getMessages(id)
            .onEach {
                msgs ->
                _uiState.update {
                    it.copy(messages = msgs, isLoading = false)
                }
                msgs.filter {
                    !it.isRead && it.senderId == SENDER_ADMIN
                }
                .forEach {
                    chatRepo.markAsRead(id, it.id)
                }
            }
            .launchIn(viewModelScope)
            pendingDao.getAll(id)
            .onEach {
                p -> _uiState.update {
                    it.copy(pendingMessages = p)
                }
            }
            .launchIn(viewModelScope)
        }
    }

    fun updateInput(text: String) = _uiState.update {
        it.copy(inputText = text)
    }

    fun sendOrSaveMessage() {
        val editing = _uiState.value.editingMessage
        if (editing != null) saveEdit(editing) else sendNewMessage()
    }

    private fun sendNewMessage() {
        val text = _uiState.value.inputText.trim()
        val id = _uiState.value.merchantId
        if (text.isEmpty() || id.isEmpty()) return
        val tempId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(inputText = "")
        }
        viewModelScope.launch {
            pendingDao.insert(PendingMessageEntity(tempId = tempId, merchantId = id, text = text, senderName = "تاجر"))
            try {
                chatRepo.sendMessage(id, ChatMessage(text = text, senderId = id, senderName = "تاجر"))
                pendingDao.delete(tempId)
            } catch (e: Exception) {
                pendingDao.setFailed(tempId, true)
            }
        }
    }

    fun retryMessage(tempId: String) {
        val msg = _uiState.value.pendingMessages.find {
            it.tempId == tempId
        } ?: return
        val id = _uiState.value.merchantId
        viewModelScope.launch {
            pendingDao.setFailed(tempId, false)
            try {
                chatRepo.sendMessage(id, ChatMessage(text = msg.text, senderId = id, senderName = "تاجر"))
                pendingDao.delete(tempId)
            } catch (e: Exception) {
                pendingDao.setFailed(tempId, true)
            }
        }
    }

    fun dismissFailedMessage(tempId: String) {
        viewModelScope.launch {
            pendingDao.delete(tempId)
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

    fun toggleSelection(messageId: String) {
        _uiState.update {
            val set = it.selectedIds.toMutableSet()
            if (messageId in set) set.remove(messageId) else set.add(messageId)
            it.copy(selectedIds = set, isSelectionMode = set.isNotEmpty())
        }
    }

    fun cancelSelection() = _uiState.update {
        it.copy(selectedIds = emptySet(), isSelectionMode = false)
    }

    // ── تعديل ────────────────────────────────────────────────────

    fun startEditSelected() {
        val msg = _uiState.value.selectedMessages.firstOrNull() ?: return
        if (msg.senderId != _uiState.value.merchantId) return
        _uiState.update {
            it.copy(editingMessage = msg, inputText = msg.text, selectedIds = emptySet(), isSelectionMode = false)
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
        val id = _uiState.value.merchantId
        _uiState.update {
            it.copy(editingMessage = null, inputText = "", isSelectionMode = false, selectedIds = emptySet())
        }
        viewModelScope.launch {
            try {
                chatRepo.editMessage(id, msg.id, newText)
            } catch (_: Exception) {}
        }
    }

    // ── نسخ — يدمج نصوص جميع المحددات بـ newline ────────────────

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
            it.senderId != state.merchantId
        }.map {
            it.id
        }.toSet()
        val ownIds = selected.filter {
            it.senderId == state.merchantId
        }.map {
            it.id
        }.toSet()

        // رسائل الطرف الآخر → حذف محلي فوري
        if (othersIds.isNotEmpty()) {
            _uiState.update {
                it.copy(locallyDeletedIds = it.locallyDeletedIds + othersIds)
            }
        }

        if (ownIds.isNotEmpty()) {
            // رسائلي → اعرض dialog
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
        val merchantId = _uiState.value.merchantId
        // إخفاء محلي فوري (حتى مع ضعف الاتصال يبدو حذفه فورياً)
        _uiState.update {
            it.copy(locallyDeletedIds = it.locallyDeletedIds + ids,
                pendingOwnDeleteIds = emptySet(), showDeleteDialog = false)
        }
        viewModelScope.launch {
            ids.forEach {
                id ->
                try {
                    chatRepo.deleteMessage(merchantId, id)
                    // Firebase soft-delete نجح → أزل من locallyDeleted ليظهر "تم حذف هذه الرسالة"
                    _uiState.update {
                        it.copy(locallyDeletedIds = it.locallyDeletedIds - id)
                    }
                } catch (_: Exception) {
                    // offline: الرسالة مخفية محلياً — TODO: PendingDeleteEntity for retry
                }
            }
        }
    }

    fun dismissDeleteDialog() = _uiState.update {
        it.copy(showDeleteDialog = false, pendingOwnDeleteIds = emptySet())
    }
}