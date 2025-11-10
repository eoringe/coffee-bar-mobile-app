package com.example.backend.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Service for handling push notifications via Firebase Cloud Messaging (FCM)
 */
class NotificationService {

    /**
     * Sends a push notification to a user's device(s)
     */
    suspend fun sendNotificationToUser(
        userUid: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val deviceTokens = getDeviceTokensForUser(userUid)
            
            if (deviceTokens.isEmpty()) {
                println("⚠️ [NotificationService] No device tokens found for user $userUid")
                return false
            }

            var successCount = 0
            deviceTokens.forEach { token ->
                try {
                    val message = Message.builder()
                        .setToken(token)
                        .setNotification(
                            Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build()
                        )
                        .putAllData(data)
                        .build()

                    val response = FirebaseMessaging.getInstance().send(message)
                    println("✅ [NotificationService] Notification sent to $userUid: $response")
                    successCount++
                } catch (e: Exception) {
                    println("❌ [NotificationService] Failed to send notification to token $token: ${e.message}")
                    // If token is invalid, we might want to remove it from DB
                    // For now, just log the error
                }
            }

            successCount > 0
        } catch (e: Exception) {
            println("❌ [NotificationService] Error sending notification: ${e.message}")
            false
        }
    }

    /**
     * Sends order ready notification
     */
    suspend fun sendOrderReadyNotification(userUid: String, orderId: Int) {
        sendNotificationToUser(
            userUid = userUid,
            title = "Order Ready! 🎉",
            body = "Your order #$orderId is ready for pickup!",
            data = mapOf(
                "type" to "order_ready",
                "orderId" to orderId.toString()
            )
        )
    }

    /**
     * Gets all device tokens for a user
     * Note: This assumes you have a UserDevices table. For now, we'll return empty list
     * You'll need to implement device token registration endpoint
     */
    private fun getDeviceTokensForUser(userUid: String): List<String> {
        // TODO: Implement when UserDevices table is created
        // For now, return empty list - you'll need to add device token registration
        return emptyList()
    }

    /**
     * Registers a device token for a user
     * This should be called when a user logs in or opens the app
     */
    fun registerDeviceToken(userUid: String, deviceToken: String, platform: String = "android"): Boolean {
        // TODO: Implement when UserDevices table is created
        // For now, just log
        println("📱 [NotificationService] Registering device token for user $userUid: $deviceToken")
        return true
    }
}



