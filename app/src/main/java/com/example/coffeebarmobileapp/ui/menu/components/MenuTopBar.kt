package com.example.coffeebarmobileapp.ui.menu.components


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTopBar() {
    TopAppBar(
        title = { Text("Menu") }, // you can change this text later
        actions = {
            IconButton(onClick = { /* later: open cart */ }) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }
    )
}
