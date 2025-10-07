package com.example

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
import org.jetbrains.exposed.sql.transactions.transaction
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

    val dbUrl = props.getProperty("database.url")
        ?: throw IllegalStateException("database.url not found in configuration")
    val dbDriver = props.getProperty("database.driver")
        ?: throw IllegalStateException("database.driver not found in configuration")
    val dbUser = props.getProperty("database.user")
        ?: throw IllegalStateException("database.user not found in configuration")
    val dbPassword = props.getProperty("database.password")
        ?: throw IllegalStateException("database.password not found in configuration")

    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )

    transaction {
        println("✅ Connected to database successfully!")
    }

    // Configure JSON serialization
    install(ContentNegotiation) {
        jackson()
    }

    // Initialize Firebase
    configureFirebase()

    // Configure Authentication
    install(Authentication) {
        firebase("firebase-auth") {
            validate { token ->
                verifyFirebaseToken(token)
            }
        }
    }

    routing {
        // Public routes
        get("/") {
            call.respond(mapOf("message" to "Coffee Bar API is running!"))
        }

        get("/health") {
            call.respond(mapOf("status" to "OK"))
        }

        // Protected routes
        authenticate("firebase-auth") {
            get("/user/profile") {
                val user = call.principal<FirebaseUser>()
                call.respond(mapOf(
                    "uid" to user?.uid,
                    "email" to user?.email,
                    "name" to user?.name,
                    "picture" to user?.picture,
                    "emailVerified" to user?.emailVerified
                ))
            }

            get("/protected") {
                val user = call.principal<FirebaseUser>()
                call.respond(mapOf(
                    "message" to "Hello ${user?.email}! This is a protected route.",
                    "uid" to user?.uid
                ))
            }
        }
    }
}