package com.example.coffeebarmobileapp.ui.menu.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

@Composable
fun MenuBottomNav() {

    val items = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Menu", Icons.Default.Coffee),
        NavItem("Favorites", Icons.Default.Favorite),
        NavItem("Profile", Icons.Default.Person)
    )

    var selectedIndex by remember { mutableStateOf(1) } // Menu selected initially

    NavigationBar(

    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex

            NavigationBarItem(
                selected = isSelected,
                onClick = { selectedIndex = index },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF3A322C) else Color(0xFF8D6E63) // brown vs muted coffee
                    )
                },
                label = {
                    Text(
                        item.label,
                        color = if (isSelected) Color(0xFF3A322C) else Color(0xFF8D6E63)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFD8C3B5) // soft light brown bubble under selected
                )
            )
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)
