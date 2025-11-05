package com.example.backend.controllers

import com.example.backend.services.ReceiptService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

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
}