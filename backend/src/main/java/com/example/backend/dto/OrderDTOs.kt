package com.example.backend.dto

data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val phoneNumber: String
)

data class OrderItemRequest(
    val menuItemId: Int,
    val size: String, // single | double
    val quantity: Int
)

data class CreateOrderResponse(
    val orderId: Int,
    val checkoutRequestID: String?,
    val merchantRequestID: String?,
    val status: String,
    val message: String
)

data class OrderResponse(
    val id: Int,
    val status: String,
    val totalAmount: Int,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val menuItemId: Int,
    val size: String,
    val quantity: Int,
    val unitPrice: Int,
    val lineTotal: Int
)


