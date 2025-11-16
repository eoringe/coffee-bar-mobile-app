package com.example

import com.example.backend.controllers.DarajaController
import com.example.backend.controllers.OrderController
import com.example.backend.controllers.ReceiptController
import com.example.backend.controllers.getMenuItems
import com.example.backend.models.*
import com.example.backend.services.DarajaService
import com.example.backend.services.OrderService
import com.example.backend.services.ReceiptService
import com.example.plugins.FirebaseUser
import com.example.plugins.configureFirebase
import com.example.plugins.firebase
import com.example.plugins.verifyFirebaseToken
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

@Suppress("unused")
fun Application.module() {

    // --- KOIN (inject, module, install) IS GONE ---

    // --- 1. CONFIG LOADING ---
    val props = Properties()
    val configFile = File("src/main/resources/application.properties")
    configFile.inputStream().use { props.load(it) }

    val dbUrl = props.getProperty("database.url")
    val dbDriver = props.getProperty("database.driver")
    val dbUser = props.getProperty("database.user")
    val dbPassword = props.getProperty("database.password")
    val darajaConsumerKey = props.getProperty("daraja.consumerKey")
    val darajaConsumerSecret = props.getProperty("daraja.consumerSecret")
    val darajaPasskey = props.getProperty("daraja.passkey")
    val darajaBusinessShortCode = props.getProperty("daraja.businessShortCode").toLong()
    val darajaCallbackUrl = props.getProperty("daraja.callbackUrl")
    // This line was missing, add it
    val ktorHost = props.getProperty("ktor.deployment.host", "0.0.0.0")

    // --- 2. DATABASE CONNECTION ---
    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )

    transaction {
        println("✅ Connected to database successfully!")
        SchemaUtils.createMissingTablesAndColumns(Categories, MenuItems, Orders, OrderItems, Receipts)
    }

    // --- 3. MANUAL INSTANTIATION ---
    val darajaService = DarajaService(
        consumerKey = darajaConsumerKey,
        consumerSecret = darajaConsumerSecret,
        passkey = darajaPasskey,
        businessShortCode = darajaBusinessShortCode,
        callbackUrl = darajaCallbackUrl
    )
    val receiptService = ReceiptService()
    val orderService = OrderService(darajaService, receiptService) // <-- Pass the real service

    val darajaController = DarajaController(darajaService) { checkoutId, success, receipt ->
        println("--- [Application.kt] CALLBACK received via controller lambda ---")
        orderService.updateOrderPaymentStatusByCheckoutId(checkoutId, success, receipt)
    }
    val orderController = OrderController(orderService)
    val receiptController = ReceiptController(receiptService) // <-- Manually create

    // --- 4. PLUGINS ---
    install(ContentNegotiation) {
        jackson { }
    }
    configureFirebase()
    install(Authentication) {
        firebase("firebase-auth") {
            validate { token ->
                verifyFirebaseToken(token)
            }
        }
    }

    // --- 5. ROUTING (Corrected) ---
    routing {
        // Public routes
        get("/") {
            call.respond(mapOf("message" to "Coffee Bar API is running!"))
        }
        get("/menu-items") {
            getMenuItems(call)
        }
        post("/daraja/callback") {
            darajaController.handleCallback(call)
        }

        // Authenticated routes
        authenticate("firebase-auth") {
            // Orders
            post("/orders") { orderController.createOrder(call) }
            get("/orders/{id}") { orderController.getOrder(call) }

            // --- THIS IS THE FIX ---
            // The specific path MUST come before the wildcard
            get("/orders/receipts") {
                receiptController.getAllReceiptsForUser(call)
            }
            get("/orders/{id}/receipt") {
                receiptController.getReceiptForOrder(call)
            }
            // -----------------------------

            // User Profile
            get("/user/profile") {
                val user = call.principal<FirebaseUser>()
                call.respond(
                    mapOf(
                        "uid" to user?.uid,
                        "email" to user?.email,
                        "name" to user?.name
                    )
                )
            }
        }
    }
}