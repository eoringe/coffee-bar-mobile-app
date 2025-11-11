package com.example.coffeebarmobileapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeebarmobileapp.ui.home.MenuItemUiModel
import com.example.coffeebarmobileapp.ui.payment.OrderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartItem(
    val item: MenuItemUiModel,
    val quantity: Int,
    val selectedSize: String
)

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0
)

class CartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState = _uiState.asStateFlow()

    private fun getPrice(item: CartItem): Double {
        return if (item.selectedSize == "single") item.item.singlePrice else item.item.doublePrice
    }

    private fun calculateSubtotal(items: List<CartItem>): Double {
        return items.sumOf { getPrice(it) * it.quantity }
    }

    fun addToCart(item: MenuItemUiModel, size: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val existingItem = currentState.items.find { it.item.id == item.id && it.selectedSize == size }

                val newItems = if (existingItem != null) {
                    currentState.items.map {
                        if (it.item.id == item.id && it.selectedSize == size) {
                            it.copy(quantity = it.quantity + 1)
                        } else {
                            it
                        }
                    }
                } else {
                    currentState.items + CartItem(item = item, quantity = 1, selectedSize = size)
                }

                val newSubtotal = calculateSubtotal(newItems)
                currentState.copy(items = newItems, subtotal = newSubtotal)
            }
        }
    }

    fun removeFromCart(itemId: Int, size: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newItems = currentState.items.filter {
                    it.item.id != itemId || it.selectedSize != size
                }
                val newSubtotal = calculateSubtotal(newItems)
                currentState.copy(items = newItems, subtotal = newSubtotal)
            }
        }
    }

    fun updateQuantity(itemId: Int, size: String, change: Int) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newItems = currentState.items
                    .map { cartItem ->
                        if (cartItem.item.id == itemId && cartItem.selectedSize == size) {
                            cartItem.copy(quantity = cartItem.quantity + change)
                        } else {
                            cartItem
                        }
                    }
                    .filter { it.quantity > 0 }

                val newSubtotal = calculateSubtotal(newItems)
                currentState.copy(items = newItems, subtotal = newSubtotal)
            }
        }
    }

    fun updateItemSize(itemId: Int, oldSize: String, newSize: String) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val itemToUpdate = currentState.items.find { it.item.id == itemId && it.selectedSize == oldSize }
                if (itemToUpdate == null) return@update currentState

                val listWithoutOldItem = currentState.items.filterNot { it.item.id == itemId && it.selectedSize == oldSize }
                val existingNewSizeItem = listWithoutOldItem.find { it.item.id == itemId && it.selectedSize == newSize }

                val newItems: List<CartItem>
                if (existingNewSizeItem != null) {
                    newItems = listWithoutOldItem.map {
                        if (it.item.id == itemId && it.selectedSize == newSize) {
                            it.copy(quantity = it.quantity + itemToUpdate.quantity)
                        } else {
                            it
                        }
                    }
                } else {
                    newItems = listWithoutOldItem + itemToUpdate.copy(selectedSize = newSize)
                }

                val newSubtotal = calculateSubtotal(newItems)
                currentState.copy(items = newItems, subtotal = newSubtotal)
            }
        }
    }

    fun getOrderDetails(): List<OrderItem> {
        return _uiState.value.items.map {
            OrderItem(
                menuItemId = it.item.id,
                quantity = it.quantity,
                size = it.selectedSize
            )
        }
    }

    fun clearCart() {
        _uiState.value = CartUiState()
    }
}