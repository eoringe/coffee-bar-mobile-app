package com.example.backend.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:postgresql://switchback.proxy.rlwy.net:13712/railway",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "JixXvZUqumoPrzbjcapHNlpWSmTGtPan"
        )
    }
}
