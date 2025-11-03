package com.example.coffeebarmobileapp.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coffeebarmobileapp.ui.menu.components.MenuItemCard
import com.example.coffeebarmobileapp.ui.menu.components.MenuTopBar
import com.example.coffeebarmobileapp.ui.menu.components.MenuBottomNav
import com.example.coffeebarmobileapp.ui.menu.components.MenuSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen() {

    val categories = listOf("Classics", "Smoothies", "Winter Warmers", "Summer Coolers", "Signatures")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    // Temporary hardcoded data — we will move to ViewModel later
    val allItems = mapOf(
        "Classics" to listOf("Espresso", "Cappuccino"),
        "Smoothies" to listOf("Berry Blast"),
        "Winter Warmers" to listOf("Hot Chocolate"),
        "Summer Coolers" to listOf("Iced Latte"),
        "Signatures" to emptyList()
    )

    val displayItems = allItems[selectedCategory] ?: emptyList()

    Scaffold(
        topBar = { MenuTopBar() },
        bottomBar = { MenuBottomNav() }
    ) { padding ->


        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(
                    RoundedCornerShape(
                        bottomStart = 50.dp,
                        bottomEnd = 50.dp
                    )
                    )
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Image",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
            Spacer(Modifier.height(10.dp))

            // Search bar
            MenuSearchBar()
            Spacer(Modifier.height(10.dp))


            // Category chips
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                category,
                                color = if (isSelected) Color.White else Color(0xFF3A322C)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3A322C),
                            containerColor = Color(0xFFFFFFFF)
                        )
                    )
                }
            }


            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF3A322C), // dark coffee outline
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(Color(0xFFE6D3C7)) // beige background
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp) // padding INSIDE the container
                ) {
                    items(displayItems) { item ->
                        MenuItemCard(itemName = item)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }


        }
    }
}
@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    MenuScreen()
}
