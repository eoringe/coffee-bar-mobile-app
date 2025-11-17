package com.example.backend.services

import com.example.backend.models.Notifications
import com.example.backend.models.UserDevices
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** Service for handling push notifications via Firebase Cloud Messaging (FCM) */
class NotificationService {

    private val objectMapper = jacksonObjectMapper()

    /** Sends a push notification to a user's device(s) and saves it to the database */
    suspend fun sendNotificationToUser(
            userUid: String,
            title: String,
            body: String,
            type: String = "system",
            data: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            // Create enriched data payload with title and body at the beginning
            // This ensures consistency between FCM message and database record
            val enrichedDataPayload =
                    data.toMutableMap().apply {
                        put("title", title) // Add title to data
                        put("body", body) // Add body to data
                    }

            val deviceTokens = getDeviceTokensForUser(userUid)

            if (deviceTokens.isEmpty()) {
                println("⚠️ [NotificationService] No device tokens found for user $userUid")
                // Still save notification to DB even if no devices (using enriched payload)
                saveNotificationToDatabase(userUid, title, body, type, enrichedDataPayload)
                return false
            }

            var successCount = 0
            val invalidTokens = mutableListOf<String>()

            deviceTokens.forEach { token ->
                try {
                    // Send PURE DATA message (no notification payload)
                    // This ensures onMessageReceived is ALWAYS called, even when app is in
                    // background
                    val message =
                            Message.builder()
                                    .setToken(token)
                                    // REMOVED: .setNotification() - forces message to always hit
                                    // onMessageReceived
                                    .putAllData(enrichedDataPayload) // Use enriched payload
                                    .build()

                    val response = FirebaseMessaging.getInstance().send(message)
                    println("✅ [NotificationService] Notification sent to $userUid: $response")
                    successCount++
                } catch (e: FirebaseMessagingException) {
                    println(
                            "❌ [NotificationService] Failed to send notification to token $token: ${e.message}"
                    )

                    // Check if token is invalid/expired
                    val errorCodeString = e.errorCode?.toString() ?: ""
                    if (errorCodeString == "messaging/invalid-registration-token" ||
                                    errorCodeString == "messaging/registration-token-not-registered"
                    ) {
                        invalidTokens.add(token)
                    }
                } catch (e: Exception) {
                    println(
                            "❌ [NotificationService] Unexpected error sending notification: ${e.message}"
                    )
                }
            }

            // Remove invalid tokens
            if (invalidTokens.isNotEmpty()) {
                removeDeviceTokens(invalidTokens)
            }

            // Save notification to database using enriched payload (ensures consistency)
            saveNotificationToDatabase(userUid, title, body, type, enrichedDataPayload)

            successCount > 0
        } catch (e: Exception) {
            println("❌ [NotificationService] Error sending notification: ${e.message}")
            false
        }
    }

    /** Saves notification to database */
    private fun saveNotificationToDatabase(
            userUid: String,
            title: String,
            body: String,
            type: String,
            data: Map<String, String>
    ) {
        try {
            transaction {
                Notifications.insert {
                    it[Notifications.userUid] = userUid
                    it[Notifications.title] = title
                    it[Notifications.message] = body
                    it[Notifications.type] = type
                    it[Notifications.isRead] = false
                    it[Notifications.data] =
                            if (data.isNotEmpty()) objectMapper.writeValueAsString(data) else null
                    it[Notifications.createdAt] = LocalDateTime.now()
                }
            }
            println("✅ [NotificationService] Notification saved to database for user $userUid")
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to save notification to database: ${e.message}")
        }
    }

    /** Sends order ready notification (timer-based, called after 5 minutes) */
    suspend fun sendOrderReadyNotification(userUid: String, orderId: Int) {
        println(
                "🔔 [NotificationService] Sending timer-based order ready notification for user $userUid, order $orderId"
        )
        sendNotificationToUser(
                userUid = userUid,
                title = "Order Should Be Ready! ⏰",
                body = "Your order #$orderId should be ready for pickup now!",
                type = "order_ready",
                data = mapOf("type" to "order_ready", "orderId" to orderId.toString())
        )
    }

    /**
     * Schedules an order ready notification to be sent after 5 minutes This is called when an order
     * is successfully paid
     */
    fun scheduleOrderReadyNotification(userUid: String, orderId: Int) {
        println(
                "⏰ [NotificationService] Scheduling order ready notification for order $orderId (5 minutes)"
        )
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Wait 5 minutes (300,000 milliseconds)
                delay(5 * 60 * 1000L)
                println(
                        "⏰ [NotificationService] Timer expired - sending order ready notification for order $orderId"
                )
                sendOrderReadyNotification(userUid, orderId)
            } catch (e: Exception) {
                println(
                        "❌ [NotificationService] Error in scheduled notification for order $orderId: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    /** Sends payment confirmation notification */
    suspend fun sendPaymentConfirmationNotification(userUid: String, orderId: Int, amount: Int) {
        println(
                "🔔 [NotificationService] Sending payment confirmation notification for user $userUid, order $orderId, amount $amount"
        )
        sendNotificationToUser(
                userUid = userUid,
                title = "Payment Confirmed ✅",
                body = "Your payment of KSh $amount for order #$orderId has been confirmed!",
                type = "payment_confirmation",
                data =
                        mapOf(
                                "type" to "payment_confirmation",
                                "orderId" to orderId.toString(),
                                "amount" to amount.toString()
                        )
        )
    }

    /** Sends order status update notification */
    suspend fun sendOrderStatusUpdateNotification(userUid: String, orderId: Int, status: String) {
        println(
                "🔔 [NotificationService] Sending order status update notification for user $userUid, order $orderId, status $status"
        )
        val (title, body) =
                when (status) {
                    "PREPARING" ->
                            "Order Being Prepared 👨‍🍳" to
                                    "Your order #$orderId is being prepared!"
                    "READY" -> "Order Ready! 🎉" to "Your order #$orderId is ready for pickup!"
                    "COMPLETED" ->
                            "Order Completed ✅" to
                                    "Thank you! Your order #$orderId has been completed."
                    "CANCELLED" -> "Order Cancelled ❌" to "Your order #$orderId has been cancelled."
                    else ->
                            "Order Update" to
                                    "Your order #$orderId status has been updated to $status"
                }

        sendNotificationToUser(
                userUid = userUid,
                title = title,
                body = body,
                type = "order_status",
                data =
                        mapOf(
                                "type" to "order_status",
                                "orderId" to orderId.toString(),
                                "status" to status
                        )
        )
    }

    /** Gets all device tokens for a user */
    fun getDeviceTokensForUser(userUid: String): List<String> {
        return transaction {
            val tokens =
                    UserDevices.select { UserDevices.userUid eq userUid }.map {
                        it[UserDevices.deviceToken]
                    }
            println(
                    "📱 [NotificationService] Found ${tokens.size} device token(s) for user $userUid"
            )
            tokens
        }
    }

    /**
     * Registers a device token for a user This should be called when a user logs in or opens the
     * app If the token already exists, it updates the timestamp
     */
    fun registerDeviceToken(
            userUid: String,
            deviceToken: String,
            platform: String = "android"
    ): Boolean {
        return try {
            transaction {
                // Check if token already exists for this user
                val existing =
                        UserDevices.select {
                                    (UserDevices.userUid eq userUid) and
                                            (UserDevices.deviceToken eq deviceToken)
                                }
                                .singleOrNull()

                if (existing != null) {
                    // Update timestamp
                    UserDevices.update({
                        (UserDevices.userUid eq userUid) and
                                (UserDevices.deviceToken eq deviceToken)
                    }) { it[UserDevices.updatedAt] = LocalDateTime.now() }
                    println(
                            "✅ [NotificationService] Updated existing device token for user $userUid"
                    )
                } else {
                    // Insert new token
                    UserDevices.insert {
                        it[UserDevices.userUid] = userUid
                        it[UserDevices.deviceToken] = deviceToken
                        it[UserDevices.platform] = platform.lowercase()
                        it[UserDevices.createdAt] = LocalDateTime.now()
                        it[UserDevices.updatedAt] = LocalDateTime.now()
                    }
                    println("✅ [NotificationService] Registered new device token for user $userUid")
                }
            }
            true
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to register device token: ${e.message}")
            false
        }
    }

    /** Unregisters a device token (when user logs out or uninstalls app) */
    fun unregisterDeviceToken(userUid: String, deviceToken: String): Boolean {
        return try {
            transaction {
                val deleted =
                        UserDevices.deleteWhere {
                            (UserDevices.userUid eq userUid) and
                                    (UserDevices.deviceToken eq deviceToken)
                        }
                println(
                        "✅ [NotificationService] Unregistered device token for user $userUid: $deleted rows deleted"
                )
            }
            true
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to unregister device token: ${e.message}")
            false
        }
    }

    /** Removes invalid device tokens from database */
    private fun removeDeviceTokens(tokens: List<String>) {
        try {
            transaction {
                var deleted = 0
                tokens.forEach { token ->
                    deleted += UserDevices.deleteWhere { UserDevices.deviceToken eq token }
                }
                println("✅ [NotificationService] Removed $deleted invalid device token(s)")
            }
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to remove invalid tokens: ${e.message}")
        }
    }

    /** Gets all notifications for a user */
    fun getNotificationsForUser(
            userUid: String,
            limit: Int = 50,
            offset: Int = 0
    ): List<Map<String, Any?>> {
        return transaction {
            Notifications.select { Notifications.userUid eq userUid }
                    .orderBy(Notifications.createdAt to SortOrder.DESC)
                    .limit(limit, offset = offset.toLong())
                    .map {
                        mapOf(
                                "id" to it[Notifications.id],
                                "title" to it[Notifications.title],
                                "message" to it[Notifications.message],
                                "type" to it[Notifications.type],
                                "isRead" to it[Notifications.isRead],
                                "data" to
                                        it[Notifications.data]?.let { json ->
                                            @Suppress("UNCHECKED_CAST")
                                            objectMapper.readValue(json, Map::class.java) as
                                                    Map<String, Any?>
                                        },
                                "createdAt" to it[Notifications.createdAt].toString()
                        )
                    }
        }
    }

    /** Gets unread notification count for a user */
    fun getUnreadNotificationCount(userUid: String): Int {
        return transaction {
            Notifications.select {
                        (Notifications.userUid eq userUid) and (Notifications.isRead eq false)
                    }
                    .count()
                    .toInt()
        }
    }

    /** Marks a notification as read */
    fun markNotificationAsRead(userUid: String, notificationId: Int): Boolean {
        return try {
            transaction {
                val updated =
                        Notifications.update({
                            (Notifications.id eq notificationId) and
                                    (Notifications.userUid eq userUid)
                        }) { it[Notifications.isRead] = true }
                updated > 0
            }
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to mark notification as read: ${e.message}")
            false
        }
    }

    /** Marks all notifications as read for a user */
    fun markAllNotificationsAsRead(userUid: String): Boolean {
        return try {
            transaction {
                Notifications.update({ Notifications.userUid eq userUid }) {
                    it[Notifications.isRead] = true
                }
            }
            true
        } catch (e: Exception) {
            println(
                    "❌ [NotificationService] Failed to mark all notifications as read: ${e.message}"
            )
            false
        }
    }

    /** Deletes a notification */
    fun deleteNotification(userUid: String, notificationId: Int): Boolean {
        return try {
            transaction {
                val deleted =
                        Notifications.deleteWhere {
                            (Notifications.id eq notificationId) and
                                    (Notifications.userUid eq userUid)
                        }
                deleted > 0
            }
        } catch (e: Exception) {
            println("❌ [NotificationService] Failed to delete notification: ${e.message}")
            false
        }
    }
}
