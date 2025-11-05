package com.example.backend.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Receipts : Table("receipts") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id).uniqueIndex() // Ensure one receipt per order
    val receiptNumber = varchar("receipt_number", 100).uniqueIndex()
    val mpesaReceiptNumber = varchar("mpesa_receipt_number", 50).nullable()
    val paymentDate = datetime("payment_date")

    /**
     * We store the entire receipt (items, totals, etc.) as a JSON string.
     * This is highly flexible.
     * If using PostgreSQL, you can change .text() to .jsonb("receipt_data") for even better performance.
     */
    val receiptData = text("receipt_data")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}