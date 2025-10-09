package com.example.backend.models

import org.jetbrains.exposed.sql.Table

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}
