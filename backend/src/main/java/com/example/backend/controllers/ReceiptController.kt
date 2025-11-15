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
import javax.swing.SortOrder

class ReceiptController(private val receiptService: ReceiptService) {

    suspend fun getReceiptForOrder(call: ApplicationCall) {
        // In a real app, you'd check if the authenticated user owns this order

        val orderId = call.parameters["id"]?.toIntOrNull()
        if (orderId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order id"))
            return
        }

        println("--- [ReceiptController] Fetching receipt for order $orderId ---")

        val receipt = receiptService.getReceiptByOrderId(orderId)
        if (receipt == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Receipt not found or not yet generated"))
            return
        }

        call.respond(HttpStatusCode.OK, receipt)
    }

    private val objectMapper = jacksonObjectMapper()
    suspend fun getAllReceiptsForUser(call: ApplicationCall) {
        val principal = call.principal<FirebaseUser>()
        val userUid = principal?.uid ?: return call.respond(
            HttpStatusCode.Unauthorized,
            mapOf("error" to "Not authenticated")
        )

        println("--- [ReceiptController] Fetching all receipts for user $userUid ---")

        // 5. This correctly calls the service, which does the database work
        val receipts = receiptService.getReceiptsByUser(userUid)
        call.respond(HttpStatusCode.OK, receipts)
    }
}