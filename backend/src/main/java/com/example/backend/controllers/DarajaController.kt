package com.example.backend.controllers

import com.example.backend.dto.PaymentRequest
import com.example.backend.dto.StkCallback
import com.example.backend.services.DarajaService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*

class DarajaController(private val darajaService: DarajaService) {

    suspend fun initiateStkPush(call: ApplicationCall) {
        try {
            val request = call.receive<PaymentRequest>()
            if (request.phoneNumber.isBlank() || request.amount <= 0) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid phone number or amount."))
                return
            }

            val result = darajaService.initiateStkPush(
                phoneNumber = request.phoneNumber,
                amount = request.amount,
                accountReference = request.accountReference,
                transactionDesc = "Coffee Bar Purchase"
            )

            result.onSuccess { successResponse ->
                call.respond(HttpStatusCode.OK, successResponse)
            }.onFailure { error ->
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (error.message ?: "An unknown error occurred")))
            }

        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body."))
        }
    }

    suspend fun handleCallback(call: ApplicationCall) {
        try {
            val callbackData = call.receive<StkCallback>()
            val stkCallback = callbackData.body.stkCallback

            // Log the entire callback for debugging
            println("✅ --- DARAJA CALLBACK RECEIVED ---")
            println("CheckoutRequestID: ${stkCallback.checkoutRequestID}")
            println("ResultCode: ${stkCallback.resultCode}")
            println("ResultDesc: ${stkCallback.resultDesc}")

            if (stkCallback.resultCode == 0) {
                println("Payment Successful!")
                stkCallback.callbackMetadata?.item?.forEach { item ->
                    println("${item.name}: ${item.value}")
                }
                // TODO: Here you would typically find the order in your database
                // using the CheckoutRequestID and update its status to "PAID".
            } else {
                println("Payment Failed/Cancelled.")
                // TODO: Update order status to "FAILED" or "CANCELLED".
            }
            println("------------------------------------")

            // Respond to Safaricom's server to acknowledge receipt
            call.respond(HttpStatusCode.OK, mapOf("status" to "Callback received successfully"))

        } catch (e: Exception) {
            println("❌ Error processing callback: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error processing callback"))
        }
    }
}
