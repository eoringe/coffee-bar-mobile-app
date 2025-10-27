package com.example

import com.example.backend.controllers.DarajaController
import com.example.backend.controllers.OrderController
import com.example.backend.controllers.getMenuItems
import com.example.backend.services.DarajaService
import com.example.backend.services.OrderService
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
import com.example.backend.models.Categories
import com.example.backend.models.MenuItems
import com.example.backend.models.OrderItems
import com.example.backend.models.Orders
import java.io.File
import java.util.Properties

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Load config from application.properties
    val props = Properties()
    val configFile = File("src/main/resources/application.properties")

    if (!configFile.exists()) {
        throw IllegalStateException("Configuration file not found: ${configFile.absolutePath}")
    }

    configFile.inputStream().use { props.load(it) }

    // Database config
    val dbUrl = props.getProperty("database.url")
        ?: throw IllegalStateException("database.url not found in configuration")
    val dbDriver = props.getProperty("database.driver")
        ?: throw IllegalStateException("database.driver not found in configuration")
    val dbUser = props.getProperty("database.user")
        ?: throw IllegalStateException("database.user not found in configuration")
    val dbPassword = props.getProperty("database.password")
        ?: throw IllegalStateException("database.password not found in configuration")

    // Daraja config
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
        SchemaUtils.createMissingTablesAndColumns(Categories, MenuItems, Orders, OrderItems)
    }

    // ✅ Instantiate Services and Controllers
    val darajaService = DarajaService(
        consumerKey = darajaConsumerKey,
        consumerSecret = darajaConsumerSecret,
        passkey = darajaPasskey,
        businessShortCode = darajaBusinessShortCode,
        callbackUrl = darajaCallbackUrl
    )
    val orderService = OrderService(darajaService)
    val darajaController = DarajaController(darajaService) { checkoutId, success, receipt ->
        orderService.updateOrderPaymentStatusByCheckoutId(checkoutId, success, receipt)
    }
    val orderController = OrderController(orderService)


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

        // 💳 New PUBLIC endpoint to initiate payment FOR TESTING
        post("/payments/stk-push") {
            darajaController.initiateStkPush(call)
        }

        // 🔒 Protected routes
        authenticate("firebase-auth") {
            // Orders
            post("/orders") { orderController.createOrder(call) }
            get("/orders/{id}") { orderController.getOrder(call) }
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

