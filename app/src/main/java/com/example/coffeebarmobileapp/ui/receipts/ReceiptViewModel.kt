package com.example.coffeebarmobileapp.ui.receipts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeebarmobileapp.ui.payment.Receipt // This is your data class
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

// --- UI State for the LIST ---
sealed interface ReceiptsUiState {
    object Loading : ReceiptsUiState
    data class Success(val receipts: List<Receipt>) : ReceiptsUiState
    data class Error(val message: String) : ReceiptsUiState
}

// --- UI State for the DETAIL screen ---
sealed interface ReceiptDetailUiState {
    object Loading : ReceiptDetailUiState
    data class Success(val receipt: Receipt) : ReceiptDetailUiState
    data class Error(val message: String) : ReceiptsUiState, ReceiptDetailUiState
}

class ReceiptsViewModel : ViewModel() {
    // State for the list of all receipts
    private val _uiState = MutableStateFlow<ReceiptsUiState>(ReceiptsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // State for ONE selected receipt
    private val _selectedReceiptState = MutableStateFlow<ReceiptDetailUiState>(ReceiptDetailUiState.Loading)
    val selectedReceiptState = _selectedReceiptState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    // TODO: Make sure this IP is correct
    private val API_URL = "http://192.168.1.194:8080"

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = auth.currentUser?.getIdToken(true)?.await()?.token
                    BearerTokens(token ?: "", token ?: "")
                }
            }
        }
    }

    init {
        fetchReceipts() // Fetch the list on init
    }

    /**
     * This is the correct function. It fetches a LIST of receipts.
     * The server handles the filtering.
     */
    fun fetchReceipts() {
        viewModelScope.launch {
            _uiState.value = ReceiptsUiState.Loading
            try {
                // This call requires the backend to be fixed
                val receipts = client.get("$API_URL/orders/receipts").body<List<Receipt>>()
                _uiState.value = ReceiptsUiState.Success(receipts)
            } catch (e: Exception) {
                Log.e("ReceiptsViewModel", "Failed to fetch receipts", e)
                _uiState.value = ReceiptsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * This function fetches a SINGLE receipt by its ID.
     */
    fun fetchReceiptById(orderId: Int) {
        viewModelScope.launch {
            _selectedReceiptState.value = ReceiptDetailUiState.Loading
            try {
                val receipt = client.get("$API_URL/orders/$orderId/receipt").body<Receipt>()
                _selectedReceiptState.value = ReceiptDetailUiState.Success(receipt)
            } catch (e: Exception) {
                Log.e("ReceiptsViewModel", "Failed to fetch receipt $orderId", e)
                _selectedReceiptState.value = ReceiptDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}