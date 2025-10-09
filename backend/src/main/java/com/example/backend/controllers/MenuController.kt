package com.example.backend.controllers

import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.backend.models.*

suspend fun getMenuItems(call: ApplicationCall) {
    val baseImageUrl = "http://127.0.0.1:8000/storage/" // Laravel's storage URL

    val items = transaction {
        (MenuItems innerJoin Categories)
            .selectAll()
            .map { row ->
                val imagePath = row[MenuItems.imagePath]
                val imageUrl = if (imagePath != null && imagePath.isNotBlank()) {
                    baseImageUrl + imagePath
                } else null

                mapOf(
                    "id" to row[MenuItems.id],
                    "coffee_title" to row[MenuItems.coffeeTitle],
                    "single_price" to row[MenuItems.singlePrice],
                    "double_price" to row[MenuItems.doublePrice],
                    "available" to row[MenuItems.available],
                    "portion_available" to row[MenuItems.portionAvailable],
                    "image_path" to imagePath,
                    "image_url" to imageUrl,
                    "category" to mapOf(
                        "id" to row[Categories.id],
                        "name" to row[Categories.name]
                    )
                )
            }
    }

    call.respond(
        mapOf(
            "success" to true,
            "message" to "Menu items retrieved successfully",
            "data" to items
        )
    )
}
