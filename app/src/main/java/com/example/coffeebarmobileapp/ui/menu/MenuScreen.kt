package com.example.coffeebarmobileapp.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coffeebarmobileapp.ui.home.MenuItemUiModel
import com.example.coffeebarmobileapp.ui.home.MenuUiState
import com.example.coffeebarmobileapp.ui.theme.* // Import your colors

// These composables are now public so HomeScreen can see MenuScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    menuUiState: MenuUiState,
    onAddItemClick: (item: MenuItemUiModel, size: String) -> Unit
) { // This is now just the content

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // This Box is the large image at the top
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
                .background(LightBrown.copy(alpha = 0.5f)), // Use your theme color
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Image", // TODO: Add your image here
                fontSize = 16.sp,
                color = TextGrey
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
                .padding(horizontal = 16.dp) // Added some padding
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            category,
                            color = if (isSelected) White else Black
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoffeeBrown, // Use theme color
                        containerColor = White
                    ),
//                    border = FilterChipDefaults.filterChipBorder(
//                        borderColor = LightBrown
//                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // This Box contains the list of items
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = CoffeeBrown,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(LightBrown.copy(alpha = 0.3f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (displayItems.isEmpty()) {
                    item {
                        Text(
                            text = "Items will be added soon.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextGrey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(displayItems) { item ->
                        MenuItemCard(itemName = item)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * A simple Card to display a menu item.
 */
@Composable
fun MenuItemCard(itemName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, LightBrown)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(CoffeeBrown),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add $itemName",
                    tint = White
                )
            }
        }
    }
}

/**
 * A placeholder SearchBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        label = { Text("Search menu...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(30.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = LightBrown
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    // A fake state for the preview
    val previewState = MenuUiState.Success(
        listOf(
            // --- ADD categoryName TO YOUR FAKE DATA ---
            MenuItemUiModel(1, "Cappuccino", 250.0, 300.0, null, "Classics"),
            MenuItemUiModel(2, "Espresso", 200.0, 250.0, null, "Classics"),
            MenuItemUiModel(3, "Berry Blast", 350.0, 350.0, null, "Smoothies")
        )
    )
    MenuScreen(menuUiState = previewState, onAddItemClick = { _, _ -> })
}