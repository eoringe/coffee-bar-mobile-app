package com.example.coffeebarmobileapp.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.coffeebarmobileapp.ui.theme.*
import com.example.coffeebarmobileapp.ui.menu.components.MenuItemCard
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.coffeebarmobileapp.R
import com.example.coffeebarmobileapp.ui.components.MenuTopAppBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MenuViewModel = viewModel(),
    onNavigateToOrder: (itemId: Int, itemName: String, itemPrice: Int) -> Unit = { _, _, _ -> },
    onNavigateToCart: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val menuState by viewModel.menuState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val categories = listOf("Hot Coffee", "Latte", "Iced Coffee", "Smoothies", "Signatures")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    // Category images map
    val categoryImages = mapOf(
        "Hot Coffee" to R.drawable.classics,
        "Latte" to R.drawable.summer,
        "Iced Coffee" to R.drawable.winter,
        "Smoothies" to R.drawable.smoothies,
        "Signatures" to R.drawable.signatures
    )

    // Filter items by category
    val displayItems = when (menuState) {
        is MenuState.Success -> {
            (menuState as MenuState.Success).items
                .filter { it.category.equals(selectedCategory, ignoreCase = true) }
                .filter {
                    if (searchQuery. isEmpty()) true
                    else it.name.contains(searchQuery, ignoreCase = true)
                }
        }
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MenuTopAppBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it},
            isSearchActive = isSearchActive,
            onSearchActiveChange = { isSearchActive = it}
        )
        // Category Image Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                .background(LightBrown.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = categoryImages[selectedCategory],
                contentDescription = selectedCategory,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(10.dp))

        // Search bar
//        MenuSearchBar()

        Spacer(Modifier.height(10.dp))

        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
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
                        selectedContainerColor = CoffeeBrown,
                        containerColor = White
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Menu Items List
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
            when (menuState) {
                is MenuState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CoffeeBrown)
                    }
                }
                is MenuState.Error -> {
                    Text(
                        text = "Error: ${(menuState as MenuState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is MenuState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (displayItems.isEmpty()) {
                            item {
                                Text(
                                    text = "No items in this category yet.",
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
                                MenuItemCard(
                                    itemName = item.name,
                                    price = item.singlePrice,
                                    imageUrl = item.imageUrl,
                                    onOrderClick = {
                                        onNavigateToOrder(item.id, item.name, item.singlePrice)
                                    },
                                    onAddToCartClick = {
                                        onNavigateToCart()
                                    }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MenuSearchBar() {
//    OutlinedTextField(
//        value = "",
//        onValueChange = {},
//        label = { Text("Search menu...") },
//        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        shape = RoundedCornerShape(30.dp),
//        colors = OutlinedTextFieldDefaults.colors(
//            unfocusedBorderColor = LightBrown
//        )
//    )
//}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    MenuScreen()
}