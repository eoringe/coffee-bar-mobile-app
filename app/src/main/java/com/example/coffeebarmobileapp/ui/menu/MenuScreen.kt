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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.example.coffeebarmobileapp.R
import com.example.coffeebarmobileapp.ui.components.MenuTopAppBar
import com.example.coffeebarmobileapp.ui.cart.CartViewModel
import com.example.coffeebarmobileapp.ui.home.MenuItemUiModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MenuViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    showSnackbar: (String) -> Unit = {},
    onNavigateToOrder: (itemId: Int, itemName: String, itemPrice: Int) -> Unit = { _, _, _ -> },
    onNavigateToCart: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val menuState by viewModel.menuState.collectAsState()
    val isRefreshing = menuState is MenuState.Loading

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val categories = listOf("Hot Coffee", "Latte", "Iced Coffee", "Smoothies", "Signatures")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    val categoryImages = mapOf(
        "Hot Coffee" to R.drawable.classics,
        "Latte" to R.drawable.summer,
        "Iced Coffee" to R.drawable.winter,
        "Smoothies" to R.drawable.smoothies,
        "Signatures" to R.drawable.signatures
    )

    val displayItems = when (menuState) {
        is MenuState.Success -> {
            (menuState as MenuState.Success).items
                .filter { it.category.equals(selectedCategory, ignoreCase = true) }
                .filter {
                    if (searchQuery.isEmpty()) true
                    else it.name.contains(searchQuery, ignoreCase = true)
                }
        }
        else -> emptyList()
    }

    val pullRefreshState = rememberPullToRefreshState()

    // Collapsible header state
    val maxHeaderHeight = 300f
    val minHeaderHeight = 0f
    var headerHeightTarget by remember { mutableFloatStateOf(maxHeaderHeight) }

    // Animated header height with smooth transition
    val headerHeight by animateFloatAsState(
        targetValue = headerHeightTarget,
        animationSpec = tween(durationMillis = 300),
        label = "headerHeight"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // Only consume scroll when scrolling up (collapsing) and header is visible
                if (delta < 0 && headerHeightTarget > minHeaderHeight) {
                    val newHeight = headerHeightTarget + delta
                    val previousHeight = headerHeightTarget
                    headerHeightTarget = newHeight.coerceIn(minHeaderHeight, maxHeaderHeight)
                    val consumed = headerHeightTarget - previousHeight
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y

                // Only expand header when scrolling down AND we've reached the top of the list
                if (delta > 0 && headerHeightTarget < maxHeaderHeight) {
                    val newHeight = headerHeightTarget + delta
                    val previousHeight = headerHeightTarget
                    headerHeightTarget = newHeight.coerceIn(minHeaderHeight, maxHeaderHeight)
                    val consumed = headerHeightTarget - previousHeight
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }
        }
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

        // refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchMenuItems() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Collapsible image
                if (headerHeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight.dp)
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
                }

                Spacer(Modifier.height(10.dp))

                // categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .zIndex(10f)
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

                // scrollable menu items
                when (menuState) {
                    is MenuState.Loading -> {
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
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CoffeeBrown)
                        }
                    }
                    is MenuState.Error -> {
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
                    }
                    is MenuState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = 1.dp,
                                    color = CoffeeBrown,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(LightBrown.copy(alpha = 0.3f))
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
                                        onAddToCartClick = {
                                            val menuItemUiModel = MenuItemUiModel(
                                                id = item.id,
                                                name = item.name,
                                                singlePrice = item.singlePrice.toDouble(),
                                                doublePrice = item.doublePrice.toDouble(),
                                                fullImageUrl = item.imageUrl,
                                                categoryName = item.category
                                            )
                                            cartViewModel.addToCart(menuItemUiModel, "single")
                                            showSnackbar("Added ${item.name} to cart")
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
}


@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    MenuScreen()
}