package com.example.backend.services

import com.example.backend.dto.AccessTokenResponse
import com.example.backend.dto.StkPushRequest
import com.example.backend.dto.StkPushSyncResponse
import com.fasterxml.jackson.annotation.JsonProperty
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

// --- DTOs for the STK Push Query ---
// (Add these to your DTO file or keep them here)

data class StkPushQueryRequest(
    @JsonProperty("BusinessShortCode")
    val businessShortCode: Long,
    @JsonProperty("Password")
    val password: String,
    @JsonProperty("Timestamp")
    val timestamp: String,
    @JsonProperty("CheckoutRequestID")
    val checkoutRequestID: String
)

data class StkPushQueryResponse(
    @JsonProperty("ResponseCode")
    val responseCode: String, // 0 if query itself was accepted
    @JsonProperty("ResponseDescription")
    val responseDescription: String,
    @JsonProperty("MerchantRequestID")
    val merchantRequestID: String,
    @JsonProperty("CheckoutRequestID")
    val checkoutRequestID: String,
    @JsonProperty("ResultCode")
    val resultCode: String, // 0 if payment was successful
    @JsonProperty("ResultDesc")
    val resultDesc: String
)

// --- END DTOs ---


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
            println("🔒 [DarajaService] Getting new access token...")
            val keyAndSecret = "$consumerKey:$consumerSecret"
            val base64KeyAndSecret = Base64.getEncoder().encodeToString(keyAndSecret.toByteArray())

            val response: AccessTokenResponse = client.get("$sandBoxUrl/oauth/v1/generate?grant_type=client_credentials") {
                header(HttpHeaders.Authorization, "Basic $base64KeyAndSecret")
            }.body()

            accessToken = response.accessToken
            // Calculate expiry time in milliseconds
            tokenExpiryTime = System.currentTimeMillis() + (response.expiresIn.toLong() * 1000)
            println("✅ [DarajaService] New token acquired.")
        }
        return accessToken!!
    }

    private fun getTimestamp(): String = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

    private fun getPassword(timestamp: String): String {
        return Base64.getEncoder().encodeToString(
            "$businessShortCode$passkey$timestamp".toByteArray()
        )
    }

    private fun formatPhoneNumber(phoneNumber: String): Long {
        return when {
            phoneNumber.startsWith("0") -> "254" + phoneNumber.substring(1)
            phoneNumber.startsWith("+254") -> phoneNumber.substring(1)
            else -> phoneNumber
        }.toLong()
    }

    suspend fun initiateStkPush(
        phoneNumber: String,
        amount: Int,
        accountReference: String,
        transactionDesc: String
    ): Result<StkPushSyncResponse> {
        return try {
            val token = getAccessToken()
            val timestamp = getTimestamp()
            val password = getPassword(timestamp)
            val formattedPhoneNumber = formatPhoneNumber(phoneNumber)

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

            println("📲 [DarajaService] Initiating STK Push for $formattedPhoneNumber...")

            val response = client.post("$sandBoxUrl/mpesa/stkpush/v1/processrequest") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<StkPushSyncResponse>()

            if(response.responseCode == "0") {
                println("✅ [DarajaService] STK Push sent successfully. CheckoutID: ${response.checkoutRequestID}")
                Result.success(response)
            } else {
                println("❌ [DarajaService] Daraja Error: ${response.responseDescription}")
                Result.failure(Exception("Daraja Error: ${response.responseDescription}"))
            }

        } catch (e: Exception) {
            println("❌ [DarajaService] Error initiating STK Push: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * NEW FUNCTION to poll for payment status.
     */
    suspend fun queryStkPushStatus(checkoutRequestID: String): Result<StkPushQueryResponse> {
        return try {
            val token = getAccessToken()
            val timestamp = getTimestamp()
            val password = getPassword(timestamp)

            val requestBody = StkPushQueryRequest(
                businessShortCode = businessShortCode,
                password = password,
                timestamp = timestamp,
                checkoutRequestID = checkoutRequestID
            )

            println("🔄 [DarajaService] Polling status for $checkoutRequestID...")

            val response = client.post("$sandBoxUrl/mpesa/stkpushquery/v1/query") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<StkPushQueryResponse>()

            println("🔄 [DarajaService] Poll response: ${response.resultDesc}")
            Result.success(response)

        } catch (e: Exception) {
            println("❌ [DarajaService] Error querying STK status: ${e.message}")
            Result.failure(e)
        }
    }
}