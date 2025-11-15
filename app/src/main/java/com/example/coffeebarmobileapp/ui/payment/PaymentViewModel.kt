package com.example.coffeebarmobileapp.ui.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeebarmobileapp.ui.cart.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


// --- DATA CLASSES FOR PAYMENT ---
@Serializable
data class OrderItem(
    val menuItemId: Int,
    val quantity: Int,
    val size: String
)
@Serializable
data class OrderRequest(
    val items: List<OrderItem>,
    val phoneNumber: String
)

@Serializable
data class OrderResponse(
    val status: String, // "PAID", "PENDING_PAYMENT", "FAILED"
    val orderId: Int? = null,
    val message: String
)

@Serializable
data class ReceiptItem(
    val itemName: String,
    val size: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

@Serializable
data class Receipt(
    val receiptNumber: String,
    val orderId: Int,
    val paymentDate: String, // This is a full timestamp
    val mpesaReceiptNumber: String? = null,
    val customerPhoneNumber: String,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val totalAmount: Double
)

sealed interface PaymentUiState {
    object Idle : PaymentUiState
    object Loading : PaymentUiState
    data class Success(val orderId: Int) : PaymentUiState
//    data class Success(val receipt: Receipt) : PaymentUiState
    object Pending : PaymentUiState
    data class Failed(val error: String) : PaymentUiState
}

class PaymentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json {
            ignoreUnknownKeys = true // <-- This is the fix
        }) }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = auth.currentUser?.getIdToken(true)?.await()?.token
                    BearerTokens(token ?: "", token ?: "")
                }
            }
        }
    }

    // TODO: Update with your server IP
    private val API_URL = "http://192.168.1.194:8080"

    fun startPayment(cartViewModel: CartViewModel, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Loading
            val orderDetails = cartViewModel.getOrderDetails()
            val requestBody = OrderRequest(items = orderDetails, phoneNumber = phoneNumber)

            try {
                val response = client.post("$API_URL/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                val orderResponse = response.body<OrderResponse>()

                // This logic directly matches your order lifecycle
                when (response.status) {
                    HttpStatusCode.Created -> { // Scenario A: Success
                        // JUST SAVE THE ID
                        _uiState.value = PaymentUiState.Success(orderResponse.orderId!!)
                        cartViewModel.clearCart()
                    }
                    HttpStatusCode.Accepted -> { // Scenario B: Pending
                        // Server timed out, status is PENDING_PAYMENT
                        _uiState.value = PaymentUiState.Pending
                        cartViewModel.clearCart()
                    }
                    HttpStatusCode.OK -> { // Scenario C: Failed
                        // User cancelled, status is FAILED
                        _uiState.value = PaymentUiState.Failed(orderResponse.message)
                    }
                    else -> {
                        _uiState.value = PaymentUiState.Failed(orderResponse.message)
                    }
                }
            } catch (e: Exception) {
                Log.e("PaymentViewModel", "Payment failed", e)
                _uiState.value = PaymentUiState.Failed(e.message ?: "Unknown error")
            }
        }
    }

    fun resetPaymentState() {
        _uiState.value = PaymentUiState.Idle
    }
}