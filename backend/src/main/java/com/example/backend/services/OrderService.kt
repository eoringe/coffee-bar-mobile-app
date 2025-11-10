package com.example.backend.services

import com.example.backend.dto.CreateOrderRequest
import com.example.backend.dto.CreateOrderResponse
import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class OrderService(
        private val darajaService: DarajaService,
        private val receiptService: ReceiptService? = null,
        private val notificationService: NotificationService? = null
) {

    /** This function is now a suspend function to allow for polling. */
    suspend fun createOrder(userUid: String, request: CreateOrderRequest): CreateOrderResponse {
        println("--- [OrderService] 1. createOrder called ---")
        require(request.items.isNotEmpty()) { "Order must contain at least one item" }

        // 1. Calculate prices and check availability
        val pricingForItems = transaction {
            request.items.map { item ->
                val row =
                        MenuItems.select { MenuItems.id eq item.menuItemId }.singleOrNull()
                                ?: throw IllegalArgumentException(
                                        "Menu item not found: ${item.menuItemId}"
                                )

                // Check if item is available
                if (!row[MenuItems.available]) {
                    throw IllegalArgumentException(
                            "Menu item ${row[MenuItems.coffeeTitle]} is not available"
                    )
                }

                // Check if enough portions are available
                val currentPortions = row[MenuItems.portionAvailable]
                if (currentPortions < item.quantity) {
                    throw IllegalArgumentException(
                            "Insufficient portions for ${row[MenuItems.coffeeTitle]}. Available: $currentPortions, Requested: ${item.quantity}"
                    )
                }

                val unitPrice =
                        when (item.size.lowercase()) {
                            "single" -> row[MenuItems.singlePrice]
                            "double" -> row[MenuItems.doublePrice]
                            else -> throw IllegalArgumentException("Invalid size: ${item.size}")
                        }
                val lineTotal = unitPrice * item.quantity
                Triple(item, unitPrice, lineTotal)
            }
        }
        val totalAmount = pricingForItems.sumOf { it.third }
        println("--- [OrderService] 2. Total amount calculated: $totalAmount ---")

        // 2. Save order as PENDING_PAYMENT
        // We MUST do this first, so the callback has an order to update.
        var orderId: Int = -1
        transaction {
            val inserted =
                    Orders.insert { r ->
                        r[Orders.userUid] = userUid // "UNAUTHENTICATED" for testing
                        r[Orders.phoneNumber] = request.phoneNumber
                        r[Orders.totalAmount] = totalAmount
                        r[Orders.status] = "PENDING_PAYMENT" // Always start as pending
                        r[Orders.checkoutRequestId] = null
                        r[Orders.merchantRequestId] = null
                        r[Orders.mpesaReceiptNumber] = null
                        r[Orders.createdAt] = LocalDateTime.now()
                    }
            orderId = inserted[Orders.id]

            pricingForItems.forEach { (item, unitPrice, lineTotal) ->
                OrderItems.insert { r ->
                    r[OrderItems.orderId] = orderId
                    r[OrderItems.menuItemId] = item.menuItemId
                    r[OrderItems.size] = item.size.lowercase()
                    r[OrderItems.quantity] = item.quantity
                    r[OrderItems.unitPrice] = unitPrice
                    r[OrderItems.lineTotal] = lineTotal
                }
            }
        }
        println("--- [OrderService] 3. Order $orderId saved to DB as PENDING_PAYMENT ---")

        // 3. Initiate STK Push
        val stkResult =
                runCatching {
                    darajaService.initiateStkPush(
                            phoneNumber = request.phoneNumber,
                            amount = totalAmount,
                            accountReference = "ORDER-$orderId",
                            transactionDesc = "Coffee Bar Order #$orderId"
                    )
                }
                        .getOrElse { e -> Result.failure(e) }

        var checkoutRequestId: String? = null
        var merchantRequestId: String? = null
        var currentStatus = "PENDING_PAYMENT"
        var message: String = ""

        stkResult
                .onSuccess { response ->
                    println(
                            "--- [OrderService] 4. STK Push initiated. CheckoutID: ${response.checkoutRequestID} ---"
                    )
                    checkoutRequestId = response.checkoutRequestID
                    merchantRequestId = response.merchantRequestID

                    // Update the order with the CheckoutRequestID
                    transaction {
                        Orders.update({ Orders.id eq orderId }) { r ->
                            r[Orders.checkoutRequestId] = response.checkoutRequestID
                            r[Orders.merchantRequestId] = response.merchantRequestID
                        }
                    }

                    // 4. NEW: Poll for the result
                    println("--- [OrderService] 5. Starting polling loop (max 60 seconds)... ---")

                    // Poll for 60 seconds (12 polls * 5 seconds)
                    val finalPollResult =
                            withTimeoutOrNull(60_000L) {
                                var isPaymentComplete = false
                                var pollResponseCode: String? = null

                                while (!isPaymentComplete) {
                                    delay(5000L) // Wait 5 seconds between polls

                                    val queryResult =
                                            darajaService.queryStkPushStatus(
                                                    response.checkoutRequestID
                                            )

                                    queryResult
                                            .onSuccess { queryResponse ->
                                                pollResponseCode = queryResponse.resultCode
                                                when (queryResponse.resultCode) {
                                                    "0" -> { // 0 = Payment successful
                                                        println(
                                                                "--- [OrderService] 6a. POLLING: SUCCESS! Payment received. ---"
                                                        )
                                                        updateOrderPaymentStatusByCheckoutId(
                                                                checkoutRequestId =
                                                                        response.checkoutRequestID,
                                                                success = true,
                                                                mpesaReceiptNumber =
                                                                        "FROM_POLL" // Note: Query
                                                                // API doesn't
                                                                // return
                                                                // receipt
                                                                // number
                                                                )
                                                        isPaymentComplete = true
                                                    }
                                                    // All other codes are failures (1032=Cancelled,
                                                    // 1=Insufficient funds, etc.)
                                                    null,
                                                    "" -> {
                                                        println(
                                                                "--- [OrderService] 6b. POLLING: Still processing (ResultCode is null)... ---"
                                                        )
                                                    }
                                                    else -> {
                                                        println(
                                                                "--- [OrderService] 6c. POLLING: FAILED! ResultCode: ${queryResponse.resultCode} (${queryResponse.resultDesc}) ---"
                                                        )
                                                        updateOrderPaymentStatusByCheckoutId(
                                                                checkoutRequestId =
                                                                        response.checkoutRequestID,
                                                                success = false,
                                                                mpesaReceiptNumber = null
                                                        )
                                                        isPaymentComplete = true
                                                    }
                                                }
                                            }
                                            .onFailure {
                                                println(
                                                        "--- [OrderService] 6d. POLLING: Query API call failed. Will retry. ${it.message} ---"
                                                )
                                                // Don't break loop, just let it retry
                                            }
                                }
                                pollResponseCode // Return the final code
                            }

                    // 5. Check poll results
                    if (finalPollResult == "0") {
                        currentStatus = "PAID"
                        message = "Payment successful (confirmed via polling)."
                    } else if (finalPollResult != null) {
                        currentStatus = "FAILED"
                        message = "Payment failed (confirmed via polling)."
                    } else {
                        println("--- [OrderService] 7. Polling timed out. Awaiting callback. ---")
                        currentStatus = "PENDING_PAYMENT"
                        message = "Payment initiated. Awaiting confirmation."
                    }
                }
                .onFailure {
                    // This means the STK push *initiation* failed
                    println("--- [OrderService] 4b. STK Push INITIATION FAILED: ${it.message} ---")
                    transaction {
                        Orders.update({ Orders.id eq orderId }) { r -> r[Orders.status] = "FAILED" }
                    }
                    currentStatus = "FAILED"
                    message = "Failed to initiate payment: ${it.message}"
                }

        println("--- [OrderService] 8. Responding to Postman. Final Status: $currentStatus ---")
        return CreateOrderResponse(
                orderId = orderId,
                checkoutRequestID = checkoutRequestId,
                merchantRequestID = merchantRequestId,
                status = currentStatus,
                message = message
        )
    }

    /**
     * This function is now called by BOTH the polling loop AND the callback handler. It's safe to
     * call multiple times.
     */
    fun updateOrderPaymentStatusByCheckoutId(
            checkoutRequestId: String,
            success: Boolean,
            mpesaReceiptNumber: String?
    ) {
        transaction {
            val order =
                    Orders.select { Orders.checkoutRequestId eq checkoutRequestId }.singleOrNull()
                            ?: run {
                                println(
                                        "❌ [OrderService] Callback/Poll for $checkoutRequestId: Order not found!"
                                )
                                return@transaction
                            }

            // Only update if it's still pending (prevents race conditions)
            if (order[Orders.status] == "PENDING_PAYMENT") {
                val newStatus = if (success) "PAID" else "FAILED"
                println("✅ [OrderService] Updating order ${order[Orders.id]} to $newStatus")
                Orders.update({ Orders.id eq order[Orders.id] }) { r ->
                    r[Orders.status] = newStatus
                    if (mpesaReceiptNumber != null) {
                        r[Orders.mpesaReceiptNumber] = mpesaReceiptNumber
                    }
                }

                // If payment was successful, reduce portions
                if (success) {
                    reducePortionsForOrder(order[Orders.id])
                }
            } else {
                println(
                        "ℹ️ [OrderService] Order ${order[Orders.id]} already processed. Ignoring duplicate update."
                )
            }
        }
    }

    /** Reduces portion availability for all items in an order */
    private fun reducePortionsForOrder(orderId: Int) {
        transaction {
            val orderItems = OrderItems.select { OrderItems.orderId eq orderId }

            orderItems.forEach { orderItem ->
                val menuItemId = orderItem[OrderItems.menuItemId]
                val quantity = orderItem[OrderItems.quantity]

                val menuItem =
                        MenuItems.select { MenuItems.id eq menuItemId }.singleOrNull()
                                ?: run {
                                    println(
                                            "⚠️ [OrderService] Menu item $menuItemId not found when reducing portions"
                                    )
                                    return@forEach
                                }

                val currentPortions = menuItem[MenuItems.portionAvailable]
                val newPortions = (currentPortions - quantity).coerceAtLeast(0)

                MenuItems.update({ MenuItems.id eq menuItemId }) { r ->
                    r[MenuItems.portionAvailable] = newPortions
                    // If portions reach 0, mark as unavailable
                    if (newPortions == 0) {
                        r[MenuItems.available] = false
                    }
                }

                println(
                        "✅ [OrderService] Reduced portions for menu item $menuItemId: $currentPortions -> $newPortions"
                )
            }
        }
    }

    /**
     * Updates order status (for admin/barista use) Valid statuses: PAID, PREPARING, READY,
     * COMPLETED, CANCELLED
     */
    suspend fun updateOrderStatus(orderId: Int, newStatus: String): Boolean {
        val validStatuses = setOf("PAID", "PREPARING", "READY", "COMPLETED", "CANCELLED", "FAILED")
        require(newStatus in validStatuses) {
            "Invalid status: $newStatus. Valid statuses: $validStatuses"
        }

        val (userUid, oldStatus) =
                transaction {
                    val order =
                            Orders.select { Orders.id eq orderId }.singleOrNull()
                                    ?: run {
                                        println("❌ [OrderService] Order $orderId not found")
                                        return@transaction Pair(null, null)
                                    }

                    val oldStatus = order[Orders.status]
                    val userUid = order[Orders.userUid]

                    Orders.update({ Orders.id eq orderId }) { r -> r[Orders.status] = newStatus }

                    println(
                            "✅ [OrderService] Updated order $orderId status: $oldStatus -> $newStatus"
                    )
                    Pair(userUid, oldStatus)
                }

        if (userUid == null) {
            return false
        }

        // Send notification if order is ready
        if (newStatus == "READY" && oldStatus != "READY") {
            notificationService?.let { service ->
                // Launch notification in background (fire and forget)
                CoroutineScope(Dispatchers.Default).launch {
                    service.sendOrderReadyNotification(userUid, orderId)
                }
            }
        }

        return true
    }

    fun getOrderById(orderId: Int): Map<String, Any?>? {
        val orderRow =
                transaction { Orders.select { Orders.id eq orderId }.singleOrNull() } ?: return null
        val items = transaction {
            OrderItems.select { OrderItems.orderId eq orderId }.map {
                mapOf(
                        "menuItemId" to it[OrderItems.menuItemId],
                        "size" to it[OrderItems.size],
                        "quantity" to it[OrderItems.quantity],
                        "unitPrice" to it[OrderItems.unitPrice],
                        "lineTotal" to it[OrderItems.lineTotal]
                )
            }
        }

        return mapOf(
                "id" to orderRow[Orders.id],
                "status" to orderRow[Orders.status],
                "totalAmount" to orderRow[Orders.totalAmount],
                "items" to items
        )
    }
}
