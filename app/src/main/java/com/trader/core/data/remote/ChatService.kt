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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ChatService {
    private val db = FirebaseFirestore.getInstance()

    private fun messagesRef(merchantId: String) = db
        .collection("chat")
        .document(merchantId)
        .collection("messages")

    fun getMessages(merchantId: String): Flow<List<ChatMessage>> {
        // ✅ Defensive Guard
        if (merchantId.isBlank()) throw IllegalArgumentException("Merchant ID is required")

        return callbackFlow {
            val listener = messagesRef(merchantId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        close(error) // Propagates error to the Flow .catch block
                        return@addSnapshotListener
                    }
                    val msgs = snapshot.documents.mapNotNull { doc ->
                        ChatMessage(
                            id = doc.id,
                            text = doc.getString("text") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            timestamp = doc.getTimestamp("timestamp"),
                            isRead = doc.getBoolean("isRead") == true,
                            readAt = doc.getLong("readAt"),
                            editedAt = doc.getTimestamp("editedAt"),
                            deletedAt = doc.getTimestamp("deletedAt")
                        )
                    }
                    trySend(msgs)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun sendMessage(merchantId: String, message: ChatMessage) {
        if (merchantId.isBlank()) throw IllegalArgumentException("Cannot send message without Merchant ID")

        val timestamp = Timestamp.now()
        // ✅ Use .document(message.id).set() to unify IDs between Local DB and Firestore
        messagesRef(merchantId).document(message.id).set(
            mapOf(
                "text" to message.text,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "timestamp" to timestamp,
                "isRead" to false,
                "readAt" to null,
                "editedAt" to null,
                "deletedAt" to null
            )
        ).await()

        if (message.senderId == SENDER_ADMIN) {
            runCatching {
                db.collection("notifications").document(merchantId).set(
                    mapOf(
                        "title" to "رسالة جديدة من الإدارة",
                        "body" to message.text,
                        "timestamp" to timestamp,
                        "isRead" to false
                    )
                ).await()
            }
        }
    }

    suspend fun markAsRead(merchantId: String, messageId: String) {
        if (merchantId.isBlank() || messageId.isBlank()) return
        messagesRef(merchantId)
            .document(messageId)
            .update(
                mapOf(
                    "isRead" to true,
                    "readAt" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun markAllAsRead(merchantId: String, messageIds: List<String>) {
        if (merchantId.isBlank() || messageIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val ref = messagesRef(merchantId)
        messageIds.chunked(400).forEach { chunk ->
            val batch: WriteBatch = db.batch()
            chunk.forEach { id ->
                batch.update(ref.document(id), mapOf("isRead" to true, "readAt" to now))
            }
            batch.commit().await()
        }
    }

    suspend fun editMessage(merchantId: String, messageId: String, newText: String) {
        if (merchantId.isBlank()) return
        messagesRef(merchantId).document(messageId)
            .update(mapOf("text" to newText, "editedAt" to Timestamp.now()))
            .await()
    }

    suspend fun deleteMessage(merchantId: String, messageId: String) {
        if (merchantId.isBlank()) return
        messagesRef(merchantId).document(messageId)
            .update("deletedAt", Timestamp.now())
            .await()
    }

    fun getUnreadCount(merchantId: String, excludeSenderId: String): Flow<Int> {
        // ✅ Defensive Guard: Prevents IllegalArgumentException
        if (merchantId.isBlank()) return flowOf(0)

        return callbackFlow {
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
}