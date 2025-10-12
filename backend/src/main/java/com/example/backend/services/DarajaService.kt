package com.example.backend.services

import com.example.backend.dto.AccessTokenResponse
import com.example.backend.dto.StkPushRequest
import com.example.backend.dto.StkPushSyncResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import java.text.SimpleDateFormat
import java.util.*

class DarajaService(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val passkey: String,
    private val businessShortCode: Long,
    private val callbackUrl: String
) {
    private val sandBoxUrl = "https://sandbox.safaricom.co.ke"
    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    private suspend fun getAccessToken(): String {
        // Check if token is null or expired (with a 10-second buffer)
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime - 10000) {
            val keyAndSecret = "$consumerKey:$consumerSecret"
            val base64KeyAndSecret = Base64.getEncoder().encodeToString(keyAndSecret.toByteArray())

            val response: AccessTokenResponse = client.get("$sandBoxUrl/oauth/v1/generate?grant_type=client_credentials") {
                header(HttpHeaders.Authorization, "Basic $base64KeyAndSecret")
            }.body()

            accessToken = response.accessToken
            // Calculate expiry time in milliseconds
            tokenExpiryTime = System.currentTimeMillis() + (response.expiresIn.toLong() * 1000)
        }
        return accessToken!!
    }

    suspend fun initiateStkPush(
        phoneNumber: String,
        amount: Int,
        accountReference: String,
        transactionDesc: String
    ): Result<StkPushSyncResponse> {
        return try {
            val token = getAccessToken()
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val password = Base64.getEncoder().encodeToString(
                "$businessShortCode$passkey$timestamp".toByteArray()
            )

            // Normalize phone number to 254... format
            val formattedPhoneNumber = when {
                phoneNumber.startsWith("0") -> "254" + phoneNumber.substring(1)
                phoneNumber.startsWith("+254") -> phoneNumber.substring(1)
                else -> phoneNumber
            }.toLong()

            val requestBody = StkPushRequest(
                businessShortCode = businessShortCode,
                password = password,
                timestamp = timestamp,
                amount = amount,
                partyA = formattedPhoneNumber,
                partyB = businessShortCode,
                phoneNumber = formattedPhoneNumber,
                callBackURL = callbackUrl,
                accountReference = accountReference,
                transactionDesc = transactionDesc
            )

            val response = client.post("$sandBoxUrl/mpesa/stkpush/v1/processrequest") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<StkPushSyncResponse>()

            if(response.responseCode == "0") {
                Result.success(response)
            } else {
                Result.failure(Exception("Daraja Error: ${response.responseDescription}"))
            }

        } catch (e: Exception) {
            println("❌ Error initiating STK Push: ${e.message}")
            Result.failure(e)
        }
    }
}
