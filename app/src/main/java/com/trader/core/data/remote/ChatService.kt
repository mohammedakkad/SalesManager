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

    fun getMessages(merchantId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.collection("chat").document(merchantId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
        val listener = ref.addSnapshotListener { snap, _ ->
            val msgs = snap?.documents?.mapNotNull { doc ->
                ChatMessage(
                    id = doc.id,
                    text = doc.getString("text") ?: "",
                    senderId = doc.getString("senderId") ?: "",
                    senderName = doc.getString("senderName") ?: "",
                    timestamp = doc.getTimestamp("timestamp"),
                    isRead = doc.getBoolean("isRead") ?: false
                )
            } ?: emptyList()
            trySend(msgs)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(merchantId: String, message: ChatMessage) {
        db.collection("chat").document(merchantId).collection("messages").add(
            mapOf("text" to message.text, "senderId" to message.senderId,
                "senderName" to message.senderName, "timestamp" to Timestamp.now(), "isRead" to false)
        ).await()
    }

    suspend fun markAsRead(merchantId: String, messageId: String) {
        db.collection("chat").document(merchantId).collection("messages")
            .document(messageId).update("isRead", true).await()
    }

    fun getUnreadCount(merchantId: String): Flow<Int> = callbackFlow {
        val ref = db.collection("chat").document(merchantId).collection("messages")
            .whereEqualTo("isRead", false).whereNotEqualTo("senderId", "admin")
        val listener = ref.addSnapshotListener { snap, _ -> trySend(snap?.size() ?: 0) }
        awaitClose { listener.remove() }
    }
}
