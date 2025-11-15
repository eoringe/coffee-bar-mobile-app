package com.example

import com.example.backend.controllers.DarajaController
import com.example.backend.controllers.OrderController
import com.example.backend.controllers.ReceiptController
import com.example.backend.controllers.getMenuItems
import com.example.backend.models.Categories
import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import com.example.backend.models.Receipts
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

fun Application.module() {
    // --- All your config loading (DB, Daraja) goes here ---
    // (Omitted for brevity, no changes)
    val props = Properties()
    val configFile = File("src/main/resources/application.properties")
    configFile.inputStream().use { props.load(it) }
    // (Load all properties...)
    val dbUrl = props.getProperty("database.url")
    val dbDriver = props.getProperty("database.driver")
    val dbUser = props.getProperty("database.user")
    val dbPassword = props.getProperty("database.password")
    val darajaConsumerKey = props.getProperty("daraja.consumerKey")
    val darajaConsumerSecret = props.getProperty("daraja.consumerSecret")
    val darajaPasskey = props.getProperty("daraja.passkey")
    val darajaBusinessShortCode = props.getProperty("daraja.businessShortCode").toLong()
    val darajaCallbackUrl = props.getProperty("daraja.callbackUrl")

    // ✅ Connect to PostgreSQL
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

    // ✅ Instantiate Services and Controllers
    val darajaService = DarajaService(
        consumerKey = darajaConsumerKey,
        consumerSecret = darajaConsumerSecret,
        passkey = darajaPasskey,
        businessShortCode = darajaBusinessShortCode,
        callbackUrl = darajaCallbackUrl
    )
    // --- Instantiate new ReceiptService ---
    val receiptService = ReceiptService()

    // --- Inject ReceiptService into OrderService ---
    val orderService = OrderService(darajaService, receiptService)

    val darajaController = DarajaController(darajaService) { checkoutId, success, receipt ->
        println("--- [Application.kt] CALLBACK received via controller lambda ---")
        orderService.updateOrderPaymentStatusByCheckoutId(checkoutId, success, receipt)
    }
    val orderController = OrderController(orderService)

    // --- Instantiate new ReceiptController ---
    val receiptController = ReceiptController(receiptService)


    // ✅ Configure JSON serialization
    install(ContentNegotiation) {
        jackson {
            // Optional: configure jackson mapper
        }
    }

    // ✅ Initialize Firebase
    configureFirebase()

    // ✅ Configure Authentication
    install(Authentication) {
        firebase("firebase-auth") {
            validate { token ->
                verifyFirebaseToken(token)
            }
        }
    }

    // ✅ ROUTES
    routing {
        // 🌍 Public routes
        get("/") {
            call.respond(mapOf("message" to "Coffee Bar API is running!"))
        }

        get("/health") {
            call.respond(mapOf("status" to "OK"))
        }

        // ☕ Public endpoint for users to view menu
        get("/menu-items") {
            getMenuItems(call)
        }

        // 📞 Public endpoint for Daraja to send callbacks
        post("/daraja/callback") {
            darajaController.handleCallback(call)
        }

        // 💳 Public endpoint to initiate payment FOR TESTING
        post("/payments/stk-push") {
            darajaController.initiateStkPush(call)
        }

        // --- 🔒 PROTECTED ROUTES ARE NOW PUBLIC ---
        authenticate("firebase-auth") {
            // Orders
            post("/orders") { orderController.createOrder(call) }
            get("/orders/{id}") { orderController.getOrder(call) }

            // New Receipt Endpoint
            get("/orders/{id}/receipt") { receiptController.getReceiptForOrder(call) }

            get("/orders/receipts") {
                receiptController.getAllReceiptsForUser(call)
            }

            // User Profile
            get("/user/profile") {
                // --- MODIFIED: Use the real user principal ---
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