package com.example.backend.models

import org.jetbrains.exposed.sql.Table

object MenuItems : Table("menu_items") {
    val id = integer("id").autoIncrement()
    val coffeeTitle = varchar("coffee_title", 255)
    val singlePrice = integer("single_price")
    val doublePrice = integer("double_price")
    val available = bool("available")
    val portionAvailable = integer("portion_available")
    val imagePath = varchar("image_path", 255).nullable()
    val categoryId = reference("category_id", Categories.id)
    override val primaryKey = PrimaryKey(id)
}
