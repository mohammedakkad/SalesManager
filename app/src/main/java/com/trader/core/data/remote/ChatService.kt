package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.trader.core.domain.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatService {
    private val db = FirebaseFirestore.getInstance()

    private fun messagesRef(merchantId: String) = db
        .collection("chat")
        .document(merchantId)
        .collection("messages")

    fun getMessages(merchantId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = messagesRef(merchantId)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val msgs = snapshot.documents.mapNotNull { doc ->
                ChatMessage(
                    id         = doc.id,
                    text       = doc.getString("text") ?: "",
                    senderId   = doc.getString("senderId") ?: "",
                    senderName = doc.getString("senderName") ?: "",
                    timestamp  = doc.getTimestamp("timestamp"),
                    isRead     = doc.getBoolean("isRead") ?: false,
                    editedAt   = doc.getTimestamp("editedAt"),
                    deletedAt  = doc.getTimestamp("deletedAt")
                )
            }
            trySend(msgs)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(merchantId: String, message: ChatMessage) {
        messagesRef(merchantId).add(
            mapOf(
                "text"       to message.text,
                "senderId"   to message.senderId,
                "senderName" to message.senderName,
                "timestamp"  to Timestamp.now(),
                "isRead"     to false,
                "editedAt"   to null,
                "deletedAt"  to null
            )
        ).await()
    }

    suspend fun markAsRead(merchantId: String, messageId: String) {
        messagesRef(merchantId)
            .document(messageId)
            .update("isRead", true)
            .await()
    }

    /** تعديل نص رسالة */
    suspend fun editMessage(merchantId: String, messageId: String, newText: String) {
        messagesRef(merchantId)
            .document(messageId)
            .update(
                mapOf(
                    "text"     to newText,
                    "editedAt" to Timestamp.now()
                )
            ).await()
    }

    /** حذف ناعم — يحتفظ بالرسالة كـ "تم حذف هذه الرسالة" */
    suspend fun deleteMessage(merchantId: String, messageId: String) {
        messagesRef(merchantId)
            .document(messageId)
            .update(
                mapOf(
                    "text"      to "",
                    "deletedAt" to Timestamp.now()
                )
            ).await()
    }

    fun getUnreadCount(merchantId: String, excludeSenderId: String): Flow<Int> = callbackFlow {
        val ref = messagesRef(merchantId)
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", excludeSenderId)

        val listener = ref.addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.size() ?: 0)
        }
        awaitClose { listener.remove() }
    }
}
