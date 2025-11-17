package com.example.backend.controllers

import com.example.backend.models.Orders
import com.example.backend.models.Receipts
import com.example.backend.services.ReceiptService
import com.example.plugins.FirebaseUser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder

class ReceiptController(private val receiptService: ReceiptService) {

    suspend fun getReceiptForOrder(call: ApplicationCall) {
        val orderId = call.parameters["id"]?.toIntOrNull()
        if (orderId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order id"))
            return
        }

        println("--- [ReceiptController] Fetching receipt for order $orderId ---")

        try {
            val receipt = receiptService.getReceiptByOrderId(orderId)
            if (receipt == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Receipt not found or not yet generated"))
                return
            }

            // Manually serialize using Jackson to ensure proper JSON response
            call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            ) {
                objectMapper.writeValueAsString(receipt)
            }
        } catch (e: Exception) {
            println("--- [ReceiptController] Error fetching receipt: ${e.message} ---")
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to fetch receipt: ${e.message}")
            )
        }
    }

    private val objectMapper = jacksonObjectMapper()
    
    suspend fun getAllReceiptsForUser(call: ApplicationCall) {
        val principal = call.principal<FirebaseUser>()
        val userUid = principal?.uid ?: return call.respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to "Not authenticated")
        )

        println("--- [ReceiptController] Fetching all receipts for user $userUid ---")

        try {
            val receipts = receiptService.getReceiptsByUser(userUid)
            println("--- [ReceiptController] Found ${receipts.size} receipts for user $userUid ---")
            
            // Manually serialize using Jackson to ensure proper JSON response
            call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK
            ) {
                objectMapper.writeValueAsString(receipts)
            }
        } catch (e: Exception) {
            println("--- [ReceiptController] Error fetching receipts: ${e.message} ---")
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to fetch receipts: ${e.message}")
            )
        }
    }
}