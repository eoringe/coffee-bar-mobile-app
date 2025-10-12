package com.example.backend.dto

import com.fasterxml.jackson.annotation.JsonProperty

// Data class for the Daraja authentication response
data class AccessTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: String
)

// Data class for initiating the STK Push request
data class StkPushRequest(
    @JsonProperty("BusinessShortCode")
    val businessShortCode: Long,
    @JsonProperty("Password")
    val password: String,
    @JsonProperty("Timestamp")
    val timestamp: String,
    @JsonProperty("TransactionType")
    val transactionType: String = "CustomerPayBillOnline",
    @JsonProperty("Amount")
    val amount: Int,
    @JsonProperty("PartyA")
    val partyA: Long, // Phone number making the payment
    @JsonProperty("PartyB")
    val partyB: Long, // Same as BusinessShortCode
    @JsonProperty("PhoneNumber")
    val phoneNumber: Long,
    @JsonProperty("CallBackURL")
    val callBackURL: String,
    @JsonProperty("AccountReference")
    val accountReference: String,
    @JsonProperty("TransactionDesc")
    val transactionDesc: String
)

// Data class for the synchronous response after initiating an STK Push
data class StkPushSyncResponse(
    @JsonProperty("MerchantRequestID")
    val merchantRequestID: String,
    @JsonProperty("CheckoutRequestID")
    val checkoutRequestID: String,
    @JsonProperty("ResponseCode")
    val responseCode: String,
    @JsonProperty("ResponseDescription")
    val responseDescription: String,
    @JsonProperty("CustomerMessage")
    val customerMessage: String
)

// Data class for the asynchronous callback from Safaricom
data class StkCallback(
    @JsonProperty("Body")
    val body: StkCallbackBody
) {
    data class StkCallbackBody(
        @JsonProperty("stkCallback")
        val stkCallback: StkCallbackData
    )

    data class StkCallbackData(
        @JsonProperty("MerchantRequestID")
        val merchantRequestID: String,
        @JsonProperty("CheckoutRequestID")
        val checkoutRequestID: String,
        @JsonProperty("ResultCode")
        val resultCode: Int,
        @JsonProperty("ResultDesc")
        val resultDesc: String,
        @JsonProperty("CallbackMetadata")
        val callbackMetadata: CallbackMetadata? = null
    )

    data class CallbackMetadata(
        @JsonProperty("Item")
        val item: List<Item>
    )

    data class Item(
        @JsonProperty("Name")
        val name: String,
        @JsonProperty("Value")
        val value: Any? = null
    )
}

// Data class for the client request to our server
data class PaymentRequest(
    val phoneNumber: String,
    val amount: Int,
    val accountReference: String
)
