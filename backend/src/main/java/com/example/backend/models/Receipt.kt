package com.example.backend.models

import kotlinx.serialization.Serializable

// This data class matches what your Android app expects
@Serializable
data class Receipt(
    val receiptNumber: String,
    val orderId: Int,
    val paymentDate: String,
    val mpesaReceiptNumber: String?,
    val customerPhoneNumber: String,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val totalAmount: Double
)

@Serializable
data class ReceiptItem(
    val itemName: String,
    val size: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)