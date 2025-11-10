package com.example

import com.example.backend.controllers.DarajaController
import com.example.backend.controllers.OrderController
import com.example.backend.controllers.ReceiptController // <-- CHANGE 1
import com.example.backend.controllers.getMenuItems
import com.example.backend.models.Categories
import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import com.example.backend.models.Receipts // <-- CHANGE 1
import com.example.backend.services.DarajaService
import com.example.backend.services.NotificationService
import com.example.backend.services.OrderService
import com.example.backend.services.ReceiptService // <-- CHANGE 1
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
    // ... (all config loading remains the same) ...
    val props = Properties()
    val configFile = File("src/main/resources/application.properties")

    if (!configFile.exists()) {
        throw IllegalStateException("Configuration file not found: ${configFile.absolutePath}")
    }

    configFile.inputStream().use { props.load(it) }

    val dbUrl = props.getProperty("database.url")
        ?: throw IllegalStateException("database.url not found in configuration")
    val dbDriver = props.getProperty("database.driver")
        ?: throw IllegalStateException("database.driver not found in configuration")
    val dbUser = props.getProperty("database.user")
        ?: throw IllegalStateException("database.user not found in configuration")
    val dbPassword = props.getProperty("database.password")
        ?: throw IllegalStateException("database.password not found in configuration")

    val darajaConsumerKey = props.getProperty("daraja.consumerKey")
        ?: throw IllegalStateException("daraja.consumerKey not found in configuration")
    val darajaConsumerSecret = props.getProperty("daraja.consumerSecret")
        ?: throw IllegalStateException("daraja.consumerSecret not found in configuration")
    val darajaPasskey = props.getProperty("daraja.passkey")
        ?: throw IllegalStateException("daraja.passkey not found in configuration")
    val darajaBusinessShortCode = props.getProperty("daraja.businessShortCode")?.toLongOrNull()
        ?: throw IllegalStateException("daraja.businessShortCode not found or invalid in configuration")
    val darajaCallbackUrl = props.getProperty("daraja.callbackUrl")
        ?: throw IllegalStateException("daraja.callbackUrl not found in configuration")


    // ✅ Connect to PostgreSQL
    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )

    transaction {
        println("✅ Connected to database successfully!")
        // --- CHANGE 2: Add Receipts table to schema creation ---
        SchemaUtils.createMissingTablesAndColumns(Categories, MenuItems, Orders, OrderItems, Receipts)
    }

    // --- CHANGE 3: Instantiate new services/controllers ---
    val darajaService = DarajaService(
        consumerKey = darajaConsumerKey,
        consumerSecret = darajaConsumerSecret,
        passkey = darajaPasskey,
        businessShortCode = darajaBusinessShortCode,
        callbackUrl = darajaCallbackUrl
    )
    val receiptService = ReceiptService() // <-- ADDED
    val notificationService = NotificationService() // <-- ADDED

    // --- CHANGE 4: Pass ReceiptService and NotificationService into OrderService ---
    val orderService = OrderService(darajaService, receiptService, notificationService) // <-- MODIFIED

    val darajaController = DarajaController(darajaService) { checkoutId, success, receipt ->
        println("--- [Application.kt] CALLBACK received via controller lambda ---")
        orderService.updateOrderPaymentStatusByCheckoutId(checkoutId, success, receipt)
    }
    val orderController = OrderController(orderService)
    val receiptController = ReceiptController(receiptService) // <-- ADDED


    // ✅ Configure JSON serialization
    install(ContentNegotiation) {
        jackson { }
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
        get("/") { call.respond(mapOf("message" to "Coffee Bar API is running!")) }
        get("/health") { call.respond(mapOf("status" to "OK")) }
        get("/menu-items") { getMenuItems(call) }
        post("/daraja/callback") { darajaController.handleCallback(call) }
        post("/payments/stk-push") { darajaController.initiateStkPush(call) }

        // --- Routes are still public for testing ---

        // Order routes
        post("/orders") { orderController.createOrder(call) }
        get("/orders/{id}") { orderController.getOrder(call) }
        put("/orders/{id}/status") { orderController.updateOrderStatus(call) }

        // --- CHANGE 5: Add new receipt route ---
        get("/orders/{id}/receipt") { receiptController.getReceiptForOrder(call) }

        // User profile route
        get("/user/profile") {
            call.respond(
                mapOf(
                    "uid" to "DUMMY_UID",
                    "email" to "test@example.com",
                    "name" to "Test User"
                )
            )
        }
    }
}





// ================================THIS WILL BE UNCOMMENTED TO RE ENABLE ROOT PROTECTION =================================/

/*
* package com.example

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
    // --- Instantiate new ReceiptService and NotificationService ---
    val receiptService = ReceiptService()
    val notificationService = NotificationService()

    // --- Inject ReceiptService and NotificationService into OrderService ---
    val orderService = OrderService(darajaService, receiptService, notificationService)

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
            put("/orders/{id}/status") { orderController.updateOrderStatus(call) }

            // New Receipt Endpoint
            get("/orders/{id}/receipt") { receiptController.getReceipt(call) }

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
}*/