package com.example.backend.controllers

import com.example.backend.dto.CreateOrderRequest
import com.example.backend.services.OrderService
import com.example.plugins.FirebaseUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class OrderController(private val orderService: OrderService) {

    suspend fun createOrder(call: ApplicationCall) {
        // val user = call.principal<FirebaseUser>()
        // if (user?.uid == null) {
        //     call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        //     return
        // }
        // --- MODIFIED: Using a dummy UID for testing ---
        val dummyUserUid = "UNAUTHENTICATED_TEST_USER"
        println("--- [OrderController] createOrder called (unauthenticated) ---")

        val request = try { call.receive<CreateOrderRequest>() } catch (e: Exception) {
            println("--- [OrderController] Invalid body: ${e.message} ---")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid body"))
            return
        }

        try {
            // Pass the dummy UID
            val response = orderService.createOrder(dummyUserUid, request)

            // Respond based on the final status from the service
            val statusCode = when(response.status) {
                "PAID" -> HttpStatusCode.Created
                "PENDING_PAYMENT" -> HttpStatusCode.Accepted
                else -> HttpStatusCode.OK // "FAILED" or other states
            }
            call.respond(statusCode, response)

        } catch (e: IllegalArgumentException) {
            println("--- [OrderController] Bad request: ${e.message} ---")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        } catch (e: Exception) {
            println("--- [OrderController] Server error: ${e.message} ---")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    suspend fun getOrder(call: ApplicationCall) {
        // val user = call.principal<FirebaseUser>()
        // if (user?.uid == null) {
        //     call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        //     return
        // }

        val orderId = call.parameters["id"]?.toIntOrNull()
        if (orderId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order id"))
            return
        }

        val order = orderService.getOrderById(orderId)
        if (order == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            return
        }
        call.respond(order)
    }
}

//=============================THIS WILL BE UNCOMMENTED TO RE ENABLE AUTHENTICATION===========================/
/*
* package com.example.backend.controllers

import com.example.backend.dto.CreateOrderRequest
import com.example.backend.services.OrderService
import com.example.plugins.FirebaseUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class OrderController(private val orderService: OrderService) {

    suspend fun createOrder(call: ApplicationCall) {
        // --- MODIFIED: Authentication is re-enabled ---
        val user = call.principal<FirebaseUser>()
        if (user?.uid == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return
        }
        println("--- [OrderController] createOrder called by user ${user.uid} ---")

        val request = try { call.receive<CreateOrderRequest>() } catch (e: Exception) {
            println("--- [OrderController] Invalid body: ${e.message} ---")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid body"))
            return
        }

        try {
            // --- MODIFIED: Pass the real user.uid ---
            val response = orderService.createOrder(user.uid, request)

            // Respond based on the final status from the service
            val statusCode = when(response.status) {
                "PAID" -> HttpStatusCode.Created
                "PENDING_PAYMENT" -> HttpStatusCode.Accepted
                else -> HttpStatusCode.OK // "FAILED" or other states
            }
            call.respond(statusCode, response)

        } catch (e: IllegalArgumentException) {
            println("--- [OrderController] Bad request: ${e.message} ---")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        } catch (e: Exception) {
            println("--- [OrderController] Server error: ${e.message} ---")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    suspend fun getOrder(call: ApplicationCall) {
        // --- MODIFIED: Authentication is re-enabled ---
        val user = call.principal<FirebaseUser>()
        if (user?.uid == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return
        }

        val orderId = call.parameters["id"]?.toIntOrNull()
        if (orderId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid order id"))
            return
        }

        // TODO: Add check to ensure user.uid owns this orderId

        val order = orderService.getOrderById(orderId)
        if (order == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            return
        }
        call.respond(order)
    }
} */