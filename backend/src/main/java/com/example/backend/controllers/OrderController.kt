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
        val user = call.principal<FirebaseUser>()
        if (user?.uid == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return
        }

        val request = try { call.receive<CreateOrderRequest>() } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid body"))
            return
        }

        try {
            val response = orderService.createOrder(user.uid, request)
            call.respond(HttpStatusCode.OK, response)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    suspend fun getOrder(call: ApplicationCall) {
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

        val order = orderService.getOrderById(orderId)
        if (order == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Order not found"))
            return
        }
        call.respond(order)
    }
}


