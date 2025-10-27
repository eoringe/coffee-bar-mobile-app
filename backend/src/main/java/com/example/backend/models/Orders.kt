package com.example.backend.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Orders : Table("orders") {
    val id = integer("id").autoIncrement()
    val userUid = varchar("user_uid", 128)
    val phoneNumber = varchar("phone_number", 32)
    val totalAmount = integer("total_amount")
    val status = varchar("status", 32) // PENDING_PAYMENT, PAID, FAILED, CANCELLED
    val checkoutRequestId = varchar("checkout_request_id", 128).nullable()
    val merchantRequestId = varchar("merchant_request_id", 128).nullable()
    val mpesaReceiptNumber = varchar("mpesa_receipt_number", 128).nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = integer("id").autoIncrement()
    val orderId = reference("order_id", Orders.id)
    val menuItemId = reference("menu_item_id", MenuItems.id)
    val size = varchar("size", 16) // "single" | "double"
    val quantity = integer("quantity")
    val unitPrice = integer("unit_price")
    val lineTotal = integer("line_total")

    override val primaryKey = PrimaryKey(id)
}


