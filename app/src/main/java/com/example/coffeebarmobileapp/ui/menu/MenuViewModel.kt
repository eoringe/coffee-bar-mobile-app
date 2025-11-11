package com.example.coffeebarmobileapp.ui.menu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

//private const val API_SERVER_URL = "http://10.0.2.2:8080"
private const val API_SERVER_URL = "http://192.168.156.164:8080"

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
    val imageUrl: String? = null,
    val category: CategoryNetwork? = null, // Add this if your DB has category
    val available: Boolean
)
@Serializable
data class CategoryNetwork(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("category_name")
    val categoryName: String? = null // Some backends use category_name
)

@Serializable
data class MenuApiResponse(
    val success: Boolean,
    val message: String,
    val data: List<MenuItemNetwork>
)

data class MenuItemUi(
    val id: Int,
    val name: String,
    val singlePrice: Int,
    val doublePrice: Int,
    val imageUrl: String?,
    val category: String
)

sealed interface MenuState {
    object Loading : MenuState
    data class Success(val items: List<MenuItemUi>) : MenuState
    data class Error(val message: String) : MenuState
}

class MenuViewModel : ViewModel() {

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Loading)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    init {
        fetchMenuItems()
    }

    fun fetchMenuItems() {
        viewModelScope.launch {
            _menuState.value = MenuState.Loading
            try {
                val url = "$API_SERVER_URL/menu-items"
                val apiResponse = client.get(url).body<MenuApiResponse>()

                if (apiResponse.success) {
                    val uiModels = apiResponse.data
                        .filter { it.available }
                        .map { item ->
                            MenuItemUi(
                                id = item.id,
                                name = item.coffeeTitle,
                                singlePrice = item.singlePrice,
                                doublePrice = item.doublePrice,
                                imageUrl = item.imageUrl,
                                category = item.category ?.name
                                    ?: item.category?.categoryName
                                    ?: "Classics"
                            )
                        }
                    _menuState.value = MenuState.Success(uiModels)
                } else {
                    _menuState.value = MenuState.Error(apiResponse.message)
                }
            } catch (e: Exception) {
                Log.e("MenuViewModel", "Failed to fetch menu items", e)
                _menuState.value = MenuState.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
