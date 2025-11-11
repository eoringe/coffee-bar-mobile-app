package com.example.coffeebarmobileapp.ui.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeebarmobileapp.ui.cart.CartViewModel
import com.google.android.gms.auth.api.Auth
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
    val status: String,
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
    val paymentDate: String,
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
    object Pending : PaymentUiState
    data class Failed(val error: String) : PaymentUiState
}

class PaymentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json {
            ignoreUnknownKeys = true
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

    private val API_URL = "http://192.168.156.164:8080" // <-- !! CHECK YOUR IP !!

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

                when (response.status) {
                    HttpStatusCode.Created -> {
                        _uiState.value = PaymentUiState.Success(orderResponse.orderId!!)
                        cartViewModel.clearCart()
                    }
                    HttpStatusCode.Accepted -> {
                        _uiState.value = PaymentUiState.Pending
                    }
                    HttpStatusCode.OK -> {
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

    private suspend fun getReceipt(orderId: Int): Receipt {
        // This makes a new network call to get the receipt details
        Log.d("PaymentViewModel", "Fetching receipt for order $orderId")
        return client.get("$API_URL/orders/$orderId/receipt").body()
    }

    fun resetPaymentState() {
        _uiState.value = PaymentUiState.Idle
    }
}