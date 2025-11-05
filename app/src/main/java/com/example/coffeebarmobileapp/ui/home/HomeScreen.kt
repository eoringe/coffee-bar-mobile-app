package com.example.coffeebarmobileapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlin.collections.listOf
import com.example.coffeebarmobileapp.ui.auth.AuthViewModel
import android.util.Log
import androidx.compose.ui.res.painterResource
import com.example.coffeebarmobileapp.ui.profile.ProfileScreen
import com.example.coffeebarmobileapp.ui.menu.MenuScreen
import com.example.coffeebarmobileapp.ui.theme.*
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.coffeebarmobileapp.ui.components.CartTopAppBar
import com.example.coffeebarmobileapp.ui.components.CoffeeShopBottomNavigation
import com.example.coffeebarmobileapp.ui.components.CoffeeShopTopAppBar
import com.example.coffeebarmobileapp.ui.components.MenuTopAppBar
import com.example.coffeebarmobileapp.ui.components.ProfileTopAppBar
import com.example.coffeebarmobileapp.ui.components.ReceiptsTopAppBar

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit, // <-- 1. Accept a navigation lambda
    viewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel() // <-- 2. Get the AuthViewModel
) {
    var selectedItem by remember { mutableStateOf(0) }

    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val menuUiState by viewModel.menuUiState.collectAsState()

    // The "isRefreshing" state is true whenever the UI state is Loading
    val isRefreshing = menuUiState is MenuUiState.Loading

    val authState by authViewModel.state

    Scaffold(
        topBar = {
            // This 'when' block now calls your public functions
            when (selectedItem) {
                0 -> CoffeeShopTopAppBar()
                1 -> MenuTopAppBar()
                2 -> CartTopAppBar()
                3 -> ReceiptsTopAppBar()
                4 -> ProfileTopAppBar()
            }
        },
        bottomBar = {
            // This just calls your public bottom nav function
            CoffeeShopBottomNavigation(
                selectedItem = selectedItem,
                onItemSelected = { index ->
                    selectedItem = index
                }
            )
        },
        containerColor = White
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> HomeScreenContent(
                    userName = userName,
                    menuUiState = menuUiState,
                    isRefreshing = isRefreshing, // <-- Pass the refreshing state down
                    onRefresh = viewModel::fetchMenuItems // <-- Pass the refresh function
                )
                1 -> MenuScreen()
                2 -> CartScreen()
                3 -> ReceiptsScreen()
                4 -> ProfileScreen(
                    userName = userName,
                    userEmail = userEmail,
                    isLoading = authState.isLoading,
                    onLogoutClick = {
                        authViewModel.logout()
                        onNavigateToLogin()
                    },
                    onChangePasswordClick = {
                        // TODO: Handle password change logic
                    },
                    onSaveName = { newName, onSaveComplete ->
                        authViewModel.updateProfileName(newName) {
                            // 1. Tell HomeViewModel to get the new name
                            viewModel.refreshUserName()

                            // 2. Tell ProfileScreen to exit edit mode
                            onSaveComplete()
                        }
                    }
                )
            }
        }
    }
}

/**
 * The actual content of the home screen (Index 0)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    userName: String,
    menuUiState: MenuUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier // <-- modifier is for the parent
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier // <-- Don't pass the parent's modifier here
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            GreetingSection(userName = userName)
            Spacer(modifier = Modifier.height(32.dp))
            TodaySpecialSection(menuUiState = menuUiState)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- PLACEHOLDER SCREENS ---
// You can create new files for these later

@Composable
private fun MenuScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Menu Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
@Composable
private fun CartScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Cart Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
//commit this

@Composable
private fun ReceiptsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Receipts Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeeShopTopAppBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    "Coffee Bar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.LocalCafe,
                    contentDescription = "Logo",
                    modifier = Modifier.size(30.dp),
                    tint = CoffeeBrown
                )
            }
            //

        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

// Placeholder TopAppBars for other screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuTopAppBar() {
    TopAppBar(
        title = { Text("Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun CartTopAppBar() {
//    TopAppBar(
//        title = { Text("Cart", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
//        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
//    )
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun ReceiptsTopAppBar() {
//    TopAppBar(
//        title = { Text("Receipts", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
//        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
//    )
//}

@Composable
private fun GreetingSection(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CoffeeBrown,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hello, $userName ☀️",
                color = CoffeeBrown,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "What would you like to order today?",
                color = TextGrey,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun TodaySpecialSection(menuUiState: MenuUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Today’s Special",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (menuUiState is MenuUiState.Error) {
            Text(
                text = "Failed to load items. Please try again later.",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        else if (menuUiState is MenuUiState.Success) {
            if (menuUiState.items.isEmpty()) {
                Text(
                    text = "Items will be added soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGrey,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    menuUiState.items.chunked(2).forEach { itemPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SpecialItemCard(
                                itemName = itemPair[0].name,
                                itemPrice = itemPair[0].price,
                                imageUrl = itemPair[0].fullImageUrl,
                                modifier = Modifier.weight(1f)
                            )

                            if (itemPair.size > 1) {
                                SpecialItemCard(
                                    itemName = itemPair[1].name,
                                    itemPrice = itemPair[1].price,
                                    imageUrl = itemPair[1].fullImageUrl,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialItemCard(
    itemName: String,
    itemPrice: String,
    imageUrl: String?,
    modifier: Modifier = Modifier // <-- 1. Accept a modifier
) {
    // Add a log to check the URL
    Log.d("SpecialItemCard", "Loading image for $itemName: $imageUrl")

    Card(
        modifier = modifier, // <-- 2. Apply the modifier here instead of a fixed width
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBrown),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = itemName,
                modifier = Modifier
                    .height(110.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                error = painterResource(id = android.R.drawable.stat_notify_error)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = itemPrice,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                    Text(
                        text = itemName,
                        fontSize = 14.sp,
                        color = Black,
                        minLines = 2,
                        maxLines = 2
                    )
                }
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .clip(CircleShape)
                        .background(CoffeeBrown),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add to cart",
                        tint = White
                    )
                }
            }
        }
    }
}

///**
// * A stateless bottom navigation bar.
// * (This code is perfect, no changes needed)
// */
//@Composable
//private fun CoffeeShopBottomNavigation(selectedItem: Int, onItemSelected: (Int) -> Unit) {
//    val items = listOf(
//        "Home" to Icons.Filled.Home,
//        "Menu" to Icons.Filled.Menu,
//        "Cart" to Icons.Filled.ShoppingCart,
//        "Receipts" to Icons.Filled.Receipt,
//        "Profile" to Icons.Filled.Person
//    )
//
//    NavigationBar(containerColor = White) {
//        items.forEachIndexed { index, item ->
//            NavigationBarItem(
//                icon = { Icon(item.second, contentDescription = item.first) },
//                label = { Text(item.first) },
//                selected = selectedItem == index,
//                onClick = { onItemSelected(index) }, // Calls the lambda
//                colors = NavigationBarItemDefaults.colors(
//                    selectedIconColor = CoffeeBrown,
//                    selectedTextColor = CoffeeBrown,
//                    indicatorColor = LightBrown,
//                    unselectedIconColor = TextGrey,
//                    unselectedTextColor = TextGrey
//                )
//            )
//        }
//    }
//}

//@Preview(showBackground = true, widthDp = 390, heightDp = 844)
//@Composable
//private fun CoffeeShopScreenPreview() {
//    HomeScreen()
//}