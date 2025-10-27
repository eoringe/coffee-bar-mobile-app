package com.example.coffeebarmobileapp.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- URL CONSTANT ---
private const val API_SERVER_URL = "http://192.168.1.194:8080"


// --- DATA MODELS ---

@Serializable
data class MenuApiResponse(
    val success: Boolean,
    val message: String,
    val data: List<MenuItemNetwork>
)

@Serializable
data class MenuItemNetwork(
    val id: Int,
    @SerialName("coffee_title")
    val coffeeTitle: String,
    @SerialName("single_price")
    val singlePrice: Int,
    @SerialName("double_price")
    val doublePrice: Int,
    @SerialName("image_url")
    val imageUrl: String? = null
)

data class MenuItemUiModel(
    val id: Int,
    val name: String,
    val price: String,
    val fullImageUrl: String?
)

sealed interface MenuUiState {
    object Loading : MenuUiState
    data class Success(val items: List<MenuItemUiModel>) : MenuUiState
    data class Error(val message: String) : MenuUiState
}


// --- VIEWMODEL ---

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    // --- User Name State ---
    // It's OK that this starts as "Guest"
    private val _userName = MutableStateFlow(auth.currentUser?.displayName ?: "Guest")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // --- THIS IS THE FIX ---
    // Create the "live feed" listener
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        // When the user logs in, this code will run and update the name
        _userName.value = user?.displayName ?: "Guest"
        Log.d("HomeViewModel", "Auth state changed. New name: ${_userName.value}")
    }
    // -----------------------


    // --- Menu Items State ---
    private val _menuUiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val menuUiState: StateFlow<MenuUiState> = _menuUiState.asStateFlow()

    // --- Ktor HTTP Client ---
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    init {
        // --- THIS IS THE FIX (PART 2) ---
        // Start listening for auth changes *as soon as* the ViewModel is created
        auth.addAuthStateListener(authStateListener)
        // -------------------------------

        fetchMenuItems()
    }

    private fun fetchMenuItems() {
        viewModelScope.launch {
            _menuUiState.value = MenuUiState.Loading
            try {
                val url = "$API_SERVER_URL/menu-items"

                val apiResponse = client.get(url).body<MenuApiResponse>()

                if (apiResponse.success) {
                    val uiModels = apiResponse.data.map { networkItem ->
                        MenuItemUiModel(
                            id = networkItem.id,
                            name = networkItem.coffeeTitle,
                            price = "KES ${networkItem.singlePrice}",
                            fullImageUrl = networkItem.imageUrl
                        )
                    }
                    _menuUiState.value = MenuUiState.Success(uiModels)

                } else {
                    Log.e("HomeViewModel", "API Error: ${apiResponse.message}")
                    _menuUiState.value = MenuUiState.Error(apiResponse.message)
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to fetch/parse menu items", e)
                _menuUiState.value = MenuUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    override fun onCleared() {
        // --- THIS IS THE FIX (PART 3) ---
        // Stop listening when the ViewModel is destroyed to prevent memory leaks
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }
}