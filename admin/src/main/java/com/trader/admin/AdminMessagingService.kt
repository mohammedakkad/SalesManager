package com.trader.admin

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AdminMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        // Handle push notifications for new chat messages
    }
    override fun onNewToken(token: String) {
        // Update admin FCM token in Firestore
    }
}
