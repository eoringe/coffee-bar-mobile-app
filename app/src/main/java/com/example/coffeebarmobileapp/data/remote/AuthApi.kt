package com.example.coffeebarmobileapp.data.remote

import com.example.coffeebarmobileapp.data.models.User
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
//
class AuthApi {
    private val client = ApiClient.client
    private val baseUrl = ApiClient.getBaseUrl()

    /**
     * Verify user with backend using Firebase token
     */
    suspend fun verifyUser(firebaseToken: String): Result<User> {
        return try {
            val response = client.get("$baseUrl/user/profile") {
                header("Authorization", "Bearer $firebaseToken")
            }

            if (response.status == HttpStatusCode.OK) {
                val user = response.body<User>()
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to verify user: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Test public endpoint
     */
    suspend fun testConnection(): Result<String> {
        return try {
            val response = client.get("$baseUrl/health")
            Result.success("Connection successful: ${response.status}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}