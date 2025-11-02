package com.example.coffeebarmobileapp.data.remote

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    // Change this to your computer's IP address when testing on physical device
    // For emulator: use 10.0.2.2
    // For physical device: use your computer's local IP (e.g., 192.168.1.100)
     private const val BASE_URL = "http://10.0.2.2:8080"  //Uncomment this line if you are using Emulator
    //This is the IP address for Emmanuel's Phone you will need to change it
    //to your computer's IP address to securely log in, use ipconfig on cmd and put that ipv4 address
//    private const val BASE_URL = "http://10.51.16.132:8080"


    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
        }
    }

    fun getBaseUrl() = BASE_URL
}