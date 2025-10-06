package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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

    routing {
        get("/") {
            call.respondText("Coffee Bar API is running!")
        }

        get("/health") {
            call.respondText("OK")
        }
    }
}