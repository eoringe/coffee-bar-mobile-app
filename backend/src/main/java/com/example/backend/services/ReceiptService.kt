//package com.example.backend.services
//
//import com.example.backend.models.MenuItems
//import com.example.backend.models.OrderItems
//import com.example.backend.models.Orders
//import com.example.backend.models.Receipts
//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//// import org.jetbrains.exposed.sql.and // <-- REMOVED (Unused)
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.select
//import org.jetbrains.exposed.sql.transactions.transaction
//import java.time.LocalDateTime
//
//class ReceiptService {
//
//    private val objectMapper = jacksonObjectMapper()
//
//    /**
//     * This function is called by OrderService *within* its existing transaction
//     * when an order is successfully marked as PAID.
//     */
//    fun generateReceiptForOrder(orderId: Int) {
//        // Idempotency Check: Don't create a receipt if one already exists
//        val existingReceipt = Receipts.select { Receipts.orderId eq orderId }.count() > 0
//        if (existingReceipt) {
//            // --- LOGGING CHANGE ---
//            println("?? [ReceiptService] Receipt for order $orderId already exists. Skipping.")
//            return
//        }
//
//        // --- LOGGING CHANGE ---
//        println("--- [ReceiptService] Generating receipt for order $orderId... ---")
//
//        // 1. Fetch all necessary data
//        val order = Orders.select { Orders.id eq orderId }.single()
//        val orderItems = (OrderItems innerJoin MenuItems)
//            .select { OrderItems.orderId eq orderId }
//            .map {
//                // --- FIX: Changed MenuItems.name to MenuItems.coffeeTitle ---
//                mapOf(
//                    "itemName" to it[MenuItems.coffeeTitle], // <-- THIS LINE IS FIXED
//                    "size" to it[OrderItems.size],
//                    "quantity" to it[OrderItems.quantity],
//                    "unitPrice" to it[OrderItems.unitPrice],
//                    "lineTotal" to it[OrderItems.lineTotal]
//                )
//            }
//
//        val totalAmount = order[Orders.totalAmount]
//        // You could calculate tax here if needed, e.g., val tax = totalAmount * 0.16
//        val subtotal = totalAmount // Assuming totalAmount is pre-tax for simplicity
//
//        // 2. Build the receipt data map
//        val receiptDetailsMap = mapOf(
//            "receiptNumber" to "RCPT-${orderId.toString().padStart(6, '0')}",
//            "orderId" to orderId,
//            "paymentDate" to LocalDateTime.now().toString(),
//            "mpesaReceiptNumber" to order[Orders.mpesaReceiptNumber],
//            "customerPhoneNumber" to order[Orders.phoneNumber],
//            "items" to orderItems,
//            "subtotal" to subtotal,
//            "tax" to 0, // Hardcoded tax for now
//            "totalAmount" to totalAmount
//        )
//
//        // 3. Serialize map to JSON string
//        val receiptDataJson = objectMapper.writeValueAsString(receiptDetailsMap)
//
//        // 4. Insert into the database
//        Receipts.insert {
//            it[Receipts.orderId] = orderId
//            it[receiptNumber] = receiptDetailsMap["receiptNumber"] as String
//            it[mpesaReceiptNumber] = order[Orders.mpesaReceiptNumber]
//            it[paymentDate] = LocalDateTime.now()
//            it[receiptData] = receiptDataJson
//            it[createdAt] = LocalDateTime.now()
//        }
//
//        // --- LOGGING CHANGE ---
//        println("--- [ReceiptService] Successfully saved receipt for order $orderId. ---")
//    }
//
//    /**
//     * Fetches the receipt from the DB and returns its data as a Map.
//     */
//    fun getReceiptByOrderId(orderId: Int): Map<String, Any?>? {
//        return transaction {
//            val receiptRow = Receipts.select { Receipts.orderId eq orderId }.singleOrNull()
//                ?: return@transaction null
//
//            // Deserialize the JSON string back into a Map
//            val receiptDataJson = receiptRow[Receipts.receiptData]
//
//            // --- FIX: Use a specific type reference for Jackson ---
//            // This tells Jackson to return Map<String, Any?> instead of a raw Map
//            val receiptDataMap: Map<String, Any?> = objectMapper.readValue(receiptDataJson)
//
//            // Return the parsed map
//            receiptDataMap
//        }
//    }
//}

package com.example.backend.services

// --- 1. ADD ALL THESE IMPORTS ---
import com.example.backend.models.Receipt
import com.example.backend.models.ReceiptItem
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.innerJoin
// -----------------------------

import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import com.example.backend.models.Receipts
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ReceiptService {

    private val objectMapper = jacksonObjectMapper()

    fun generateReceiptForOrder(orderId: Int) {
        val existingReceipt = Receipts.select { Receipts.orderId eq orderId }.count() > 0
        if (existingReceipt) {
            println("?? [ReceiptService] Receipt for order $orderId already exists. Skipping.")
            return
        }
        println("--- [ReceiptService] Generating receipt for order $orderId... ---")

        val order = Orders.select { Orders.id eq orderId }.single()

        // --- 2. Map to ReceiptItem data class ---
        val orderItems = (OrderItems innerJoin MenuItems)
            .select { OrderItems.orderId eq orderId }
            .map {
                ReceiptItem( // <-- This is now resolved
                    itemName = it[MenuItems.coffeeTitle],
                    size = it[OrderItems.size],
                    quantity = it[OrderItems.quantity],
                    unitPrice = it[OrderItems.unitPrice].toDouble(),
                    lineTotal = it[OrderItems.lineTotal].toDouble()
                )
            }

        val totalAmount = order[Orders.totalAmount].toDouble()
        val subtotal = totalAmount

        // --- 3. Build the Receipt data class ---
        val receipt = Receipt( // <-- This is now resolved
            receiptNumber = "RCPT-${orderId.toString().padStart(6, '0')}",
            orderId = orderId,
            paymentDate = order[Orders.createdAt].toString(),
            mpesaReceiptNumber = order[Orders.mpesaReceiptNumber],
            customerPhoneNumber = order[Orders.phoneNumber], // <-- This is now resolved
            items = orderItems,
            subtotal = subtotal,
            tax = 0.0,
            totalAmount = totalAmount
        )

        val receiptDataJson = objectMapper.writeValueAsString(receipt)

        Receipts.insert {
            it[Receipts.orderId] = orderId
            it[receiptNumber] = receipt.receiptNumber
            it[mpesaReceiptNumber] = receipt.mpesaReceiptNumber
            it[paymentDate] = order[Orders.createdAt]
            it[receiptData] = receiptDataJson
            it[createdAt] = LocalDateTime.now()
        }
        println("--- [ReceiptService] Successfully saved receipt for order $orderId. ---")
    }

    fun getReceiptByOrderId(orderId: Int): Receipt? { // <-- 4. Return the data class
        return transaction {
            val receiptRow = Receipts.select { Receipts.orderId eq orderId }.singleOrNull()
                ?: return@transaction null

            val receiptDataJson = receiptRow[Receipts.receiptData]
            objectMapper.readValue<Receipt>(receiptDataJson) // <-- 5. Deserialize to data class
        }
    }

    fun getReceiptsByUser(userUid: String): List<Receipt> { // <-- 6. Return List<Receipt>
        return transaction {
            (Receipts innerJoin Orders)
                .select { Orders.userUid eq userUid }
                .orderBy(Receipts.createdAt, SortOrder.DESC)
                .map { receiptRow ->
                    val receiptDataJson = receiptRow[Receipts.receiptData]
                    objectMapper.readValue<Receipt>(receiptDataJson) // <-- 7. Deserialize to data class
                }
        }
    }
}