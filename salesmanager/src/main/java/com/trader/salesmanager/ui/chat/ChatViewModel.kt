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

// حالة الرسالة المحددة لعمليات السياق (تعديل/حذف)
data class SelectedMessageState(
    val messageId: String,
    val isOwn: Boolean,     // أنا صاحبها؟
    val currentText: String
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingMessages: List<PendingMessageEntity> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val merchantId: String = "",
    // حالة تعديل رسالة
    val editingMessage: ChatMessage? = null,
    // الرسائل المحددة للحذف المتعدد
    val selectedForDelete: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    // الرسالة المضغوطة لإظهار قائمة السياق
    val contextMessage: SelectedMessageState? = null
)

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val activationRepo: ActivationRepository,
    private val pendingDao: PendingMessageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { loadChat() }

    private fun loadChat() {
        viewModelScope.launch {
            val id = activationRepo.getMerchantCode()
            _uiState.update { it.copy(merchantId = id) }

            // مراقبة الرسائل من Firestore
            chatRepo.getMessages(id)
                .onEach { msgs ->
                    _uiState.update { it.copy(messages = msgs, isLoading = false) }
                    // ✅ Batch — تعليم كل رسائل الأدمن غير المقروءة دفعة واحدة
                    val unreadFromAdmin = msgs.filter { !it.isRead && it.senderId == SENDER_ADMIN }
                        .map { it.id }
                    if (unreadFromAdmin.isNotEmpty()) {
                        chatRepo.markAllAsRead(id, unreadFromAdmin)
                    }
                }
                .launchIn(viewModelScope)

            // مراقبة الرسائل المعلقة من Room (تبقى بعد إغلاق التطبيق)
            pendingDao.getAll(id)
                .onEach { pending ->
                    _uiState.update { it.copy(pendingMessages = pending) }
                }
                .launchIn(viewModelScope)
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    // ── إرسال رسالة جديدة أو حفظ تعديل ─────────────────────────
    fun sendOrSaveMessage() {
        val editing = _uiState.value.editingMessage
        if (editing != null) {
            saveEdit(editing)
        } else {
            sendNewMessage()
        }
    }

    private fun sendNewMessage() {
        val text = _uiState.value.inputText.trim()
        val id   = _uiState.value.merchantId
        if (text.isEmpty() || id.isEmpty()) return

        val tempId = UUID.randomUUID().toString()
        val entity = PendingMessageEntity(
            tempId     = tempId,
            merchantId = id,
            text       = text,
            senderName = "تاجر"
        )
        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            // حفظ في Room أولاً (يظهر فوراً في الـ UI ويبقى بعد الإغلاق)
            pendingDao.insert(entity)
            try {
                chatRepo.sendMessage(id, ChatMessage(text = text, senderId = id, senderName = "تاجر"))
                pendingDao.delete(tempId)  // نجح → احذف من pending
            } catch (e: Exception) {
                pendingDao.setFailed(tempId, true)  // فشل → علّم كـ failed
            }
        }
    }

    fun retryMessage(tempId: String) {
        val msg = _uiState.value.pendingMessages.find { it.tempId == tempId } ?: return
        val id  = _uiState.value.merchantId
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
        viewModelScope.launch { pendingDao.delete(tempId) }
    }

    // ── Long-press → قائمة السياق ────────────────────────────────
    fun onLongPress(msg: ChatMessage) {
        if (_uiState.value.isSelectionMode) {
            // في وضع تحديد متعدد → أضف/أزل من المحددات
            toggleSelection(msg.id)
            return
        }
        _uiState.update {
            it.copy(
                contextMessage = SelectedMessageState(
                    messageId   = msg.id,
                    isOwn       = msg.senderId == it.merchantId,
                    currentText = msg.text
                )
            )
        }
    }

    fun dismissContext() = _uiState.update { it.copy(contextMessage = null) }

    // ── تعديل رسالة ──────────────────────────────────────────────
    fun startEdit(msg: ChatMessage) {
        _uiState.update {
            it.copy(
                editingMessage = msg,
                inputText      = msg.text,
                contextMessage = null
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(editingMessage = null, inputText = "") }

    private fun saveEdit(msg: ChatMessage) {
        val newText = _uiState.value.inputText.trim()
        if (newText.isEmpty() || newText == msg.text) { cancelEdit(); return }
        val id = _uiState.value.merchantId
        _uiState.update { it.copy(editingMessage = null, inputText = "") }
        viewModelScope.launch {
            try { chatRepo.editMessage(id, msg.id, newText) } catch (_: Exception) {}
        }
    }

    // ── حذف رسالة واحدة ──────────────────────────────────────────
    fun deleteMessage(messageId: String) {
        val id = _uiState.value.merchantId
        _uiState.update { it.copy(contextMessage = null) }
        viewModelScope.launch {
            try { chatRepo.deleteMessage(id, messageId) } catch (_: Exception) {}
        }
    }

    // ── وضع التحديد المتعدد للحذف ────────────────────────────────
    fun enterSelectionMode(messageId: String) {
        _uiState.update {
            it.copy(
                isSelectionMode  = true,
                selectedForDelete = setOf(messageId),
                contextMessage   = null
            )
        }
    }

    fun toggleSelection(messageId: String) {
        _uiState.update {
            val set = it.selectedForDelete.toMutableSet()
            if (messageId in set) set.remove(messageId) else set.add(messageId)
            it.copy(
                selectedForDelete = set,
                isSelectionMode   = set.isNotEmpty()
            )
        }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedForDelete.toList()
        val merchantId = _uiState.value.merchantId
        _uiState.update { it.copy(selectedForDelete = emptySet(), isSelectionMode = false) }
        viewModelScope.launch {
            ids.forEach { id ->
                try { chatRepo.deleteMessage(merchantId, id) } catch (_: Exception) {}
            }
        }
    }

    fun cancelSelection() = _uiState.update {
        it.copy(selectedForDelete = emptySet(), isSelectionMode = false)
    }
}
