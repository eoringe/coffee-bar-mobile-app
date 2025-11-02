package com.example.backend.services

import com.example.backend.dto.CreateOrderRequest
import com.example.backend.dto.CreateOrderResponse
import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class OrderService(private val darajaService: DarajaService) {

    suspend fun createOrder(userUid: String, request: CreateOrderRequest): CreateOrderResponse {
        require(request.items.isNotEmpty()) { "Order must contain at least one item" }

        val pricingForItems = transaction {
            request.items.map { item ->
                val row =
                        MenuItems.select { MenuItems.id eq item.menuItemId }.singleOrNull()
                                ?: throw IllegalArgumentException(
                                        "Menu item not found: ${item.menuItemId}"
                                )
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

        var orderId: Int = -1
        var checkoutRequestId: String? = null
        var merchantRequestId: String? = null

        transaction {
            val inserted =
                    Orders.insert { r ->
                        r[Orders.userUid] = userUid
                        r[Orders.phoneNumber] = request.phoneNumber
                        r[Orders.totalAmount] = totalAmount
                        r[Orders.status] = "PENDING_PAYMENT"
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

        // Initiate STK Push for payment
        val stkResult =
                darajaService.initiateStkPush(
                        phoneNumber = request.phoneNumber,
                        amount = totalAmount,
                        accountReference = "ORDER-$orderId",
                        transactionDesc = "Coffee Bar Order #$orderId"
                )

        stkResult
                .onSuccess { response ->
                    checkoutRequestId = response.checkoutRequestID
                    merchantRequestId = response.merchantRequestID
                    transaction {
                        Orders.update({ Orders.id eq orderId }) { r ->
                            r[Orders.checkoutRequestId] = response.checkoutRequestID
                            r[Orders.merchantRequestId] = response.merchantRequestID
                        }
                    }
                }
                .onFailure {
                    transaction {
                        Orders.update({ Orders.id eq orderId }) { r -> r[Orders.status] = "FAILED" }
                    }
                    return CreateOrderResponse(
                            orderId = orderId,
                            checkoutRequestID = null,
                            merchantRequestID = null,
                            status = "FAILED",
                            message = "Failed to initiate payment"
                    )
                }

        return CreateOrderResponse(
                orderId = orderId,
                checkoutRequestID = checkoutRequestId,
                merchantRequestID = merchantRequestId,
                status = "PENDING_PAYMENT",
                message = "Payment initiated. Awaiting confirmation"
        )
    }

    fun updateOrderPaymentStatusByCheckoutId(
            checkoutRequestId: String,
            success: Boolean,
            mpesaReceiptNumber: String?
    ) {
        transaction {
            val order =
                    Orders.select { Orders.checkoutRequestId eq checkoutRequestId }.singleOrNull()
                            ?: return@transaction
            Orders.update({ Orders.id eq order[Orders.id] }) { r ->
                r[Orders.status] = if (success) "PAID" else "FAILED"
                r[Orders.mpesaReceiptNumber] = mpesaReceiptNumber
            }
        }
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
