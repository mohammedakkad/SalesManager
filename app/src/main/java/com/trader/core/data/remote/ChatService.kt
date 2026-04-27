package com.trader.core.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.trader.core.domain.model.ChatMessage
import com.trader.core.domain.model.SENDER_ADMIN
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
        val listener = messagesRef(merchantId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val msgs = snapshot.documents.mapNotNull { doc ->
                    ChatMessage(
                        id         = doc.id,
                        text       = doc.getString("text")       ?: "",
                        senderId   = doc.getString("senderId")   ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        timestamp  = doc.getTimestamp("timestamp"),
                        isRead     = doc.getBoolean("isRead")    ?: false,
                        readAt     = doc.getLong("readAt"),
                        editedAt   = doc.getTimestamp("editedAt"),
                        deletedAt  = doc.getTimestamp("deletedAt")
                    )
                }
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(merchantId: String, message: ChatMessage) {
        val timestamp = Timestamp.now()
        messagesRef(merchantId).add(
            mapOf(
                "text"       to message.text,
                "senderId"   to message.senderId,
                "senderName" to message.senderName,
                "timestamp"  to timestamp,
                "isRead"     to false,
                "readAt"     to null,
                "editedAt"   to null,
                "deletedAt"  to null
            )
        ).await()

        // FCM trigger for merchant
        if (message.senderId == SENDER_ADMIN) {
            runCatching {
                db.collection("notifications").document(merchantId).set(mapOf(
                    "title"     to "رسالة جديدة من الإدارة",
                    "body"      to message.text,
                    "timestamp" to timestamp,
                    "isRead"    to false
                )).await()
            }
        }
    }

    // ✅ تعليم رسالة واحدة كمقروءة مع وقت القراءة
    suspend fun markAsRead(merchantId: String, messageId: String) {
        messagesRef(merchantId)
            .document(messageId)
            .update(mapOf(
                "isRead" to true,
                "readAt" to System.currentTimeMillis()
            )).await()
    }

    // ✅ Batch — تعليم كل الرسائل غير المقروءة دفعة واحدة
    // أفضل بكثير من forEach لأنها عملية واحدة على Firebase
    suspend fun markAllAsRead(merchantId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val ref = messagesRef(merchantId)
        // Firestore يدعم 500 عملية per batch
        messageIds.chunked(400).forEach { chunk ->
            val batch: WriteBatch = db.batch()
            chunk.forEach { id ->
                batch.update(ref.document(id), mapOf("isRead" to true, "readAt" to now))
            }
            batch.commit().await()
        }
    }

    suspend fun editMessage(merchantId: String, messageId: String, newText: String) {
        messagesRef(merchantId).document(messageId)
            .update(mapOf("text" to newText, "editedAt" to Timestamp.now()))
            .await()
    }

    suspend fun deleteMessage(merchantId: String, messageId: String) {
        messagesRef(merchantId).document(messageId)
            .update("deletedAt", Timestamp.now())
            .await()
    }

    fun getUnreadCount(merchantId: String, excludeSenderId: String): Flow<Int> = callbackFlow {
        val listener = messagesRef(merchantId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.documents
                    ?.count { it.getString("senderId") != excludeSenderId } ?: 0
                trySend(count)
            }
        awaitClose { listener.remove() }
    }
}
