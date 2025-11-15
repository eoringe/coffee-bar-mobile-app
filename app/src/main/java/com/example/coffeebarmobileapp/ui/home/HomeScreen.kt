package com.example.coffeebarmobileapp.ui.home

import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.coffeebarmobileapp.ui.auth.AuthViewModel
import com.example.coffeebarmobileapp.ui.cart.CartScreen
import com.example.coffeebarmobileapp.ui.cart.CartViewModel
import com.example.coffeebarmobileapp.ui.components.CartTopAppBar
import com.example.coffeebarmobileapp.ui.components.CoffeeShopBottomNavigation
import com.example.coffeebarmobileapp.ui.components.CoffeeShopTopAppBar
import com.example.coffeebarmobileapp.ui.components.MenuTopAppBar
import com.example.coffeebarmobileapp.ui.components.ProfileTopAppBar
import com.example.coffeebarmobileapp.ui.components.ReceiptsTopAppBar
import com.example.coffeebarmobileapp.ui.menu.MenuScreen
import com.example.coffeebarmobileapp.ui.payment.PaymentScreen
import com.example.coffeebarmobileapp.ui.payment.PaymentSuccessScreen
import com.example.coffeebarmobileapp.ui.payment.PaymentViewModel
import com.example.coffeebarmobileapp.ui.payment.PaymentUiState
import com.example.coffeebarmobileapp.ui.receipts.ReceiptDetailScreen
import com.example.coffeebarmobileapp.ui.receipts.ReceiptsScreen
import com.example.coffeebarmobileapp.ui.profile.ProfileScreen
import com.example.coffeebarmobileapp.ui.theme.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.coffeebarmobileapp.ui.receipts.ReceiptsViewModel
import com.example.coffeebarmobileapp.ui.components.ReceiptDetailTopAppBar
import com.example.coffeebarmobileapp.ui.menu.MenuState
import com.example.coffeebarmobileapp.ui.menu.MenuViewModel

// --- MAIN HOME SCREEN ---
@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    paymentViewModel: PaymentViewModel = viewModel(),
    receiptsViewModel: ReceiptsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            when (currentRoute) {
                MainDestinations.HOME -> CoffeeShopTopAppBar()
//                MainDestinations.MENU -> MenuTopAppBar()
                MainDestinations.CART -> CartTopAppBar()
                MainDestinations.RECEIPTS -> ReceiptsTopAppBar()
                MainDestinations.PROFILE -> ProfileTopAppBar()
                else -> {}
            }
        },
        bottomBar = {
            when (currentRoute) {
                MainDestinations.HOME,
                MainDestinations.MENU,
                MainDestinations.CART,
                MainDestinations.RECEIPT_DETAIL,
                MainDestinations.RECEIPTS,
                MainDestinations.PROFILE -> {
                    CoffeeShopBottomNavigation(
                        currentRoute = currentRoute,
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                else -> {}
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = White
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            homeViewModel = homeViewModel,
            authViewModel = authViewModel,
            menuViewModel = menuViewModel,
            paymentViewModel = paymentViewModel,
            cartViewModel = cartViewModel,
            receiptsViewModel = receiptsViewModel,
            onNavigateToLogin = onNavigateToLogin,
            modifier = Modifier.padding(innerPadding),
            showSnackbar = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

// --- NESTED NAV GRAPH FOR THE MAIN APP ---
@Composable
fun MainNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    paymentViewModel: PaymentViewModel,
    menuViewModel: MenuViewModel,
    receiptsViewModel: ReceiptsViewModel,
    cartViewModel: CartViewModel,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    showSnackbar: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = MainDestinations.HOME,
        modifier = modifier
    ) {
        // Home Tab
        composable(MainDestinations.HOME) {
            HomeScreenContent(
                viewModel = homeViewModel,
                cartViewModel = cartViewModel,
                showSnackbar = showSnackbar
            )
        }

        // Menu Tab
        composable(MainDestinations.MENU) {
            MenuScreen(
                viewModel = menuViewModel,
                cartViewModel = cartViewModel,
                showSnackbar= showSnackbar
            )
        }

        // Cart Tab
        composable(MainDestinations.CART) {
            CartScreen(
                viewModel = cartViewModel,
                onCheckoutClick = {
                    navController.navigate(MainDestinations.PAYMENT)
                }
            )
        }

        // Receipts Tab
        composable(MainDestinations.RECEIPTS) {
            ReceiptsScreen(
                viewModel = receiptsViewModel,
                onReceiptClick = { orderId ->
                    navController.navigate("${MainDestinations.RECEIPT_DETAIL_ROUTE}/$orderId")
                }
            )
        }

        // Profile Tab
        composable(MainDestinations.PROFILE) {
            val userName by homeViewModel.userName.collectAsState()
            val userEmail by homeViewModel.userEmail.collectAsState()
            val authState by authViewModel.state

            ProfileScreen(
                userName = userName,
                userEmail = userEmail,
                isLoading = authState.isLoading,
                onLogoutClick = {
                    authViewModel.logout()
                    onNavigateToLogin()
                },
                onSaveName = { newName, onSaveComplete ->
                    authViewModel.updateProfileName(newName) {
                        homeViewModel.refreshUserName()
                        onSaveComplete()
                    }
                }
            )
        }

        // --- New Payment Flow Screens ---
        composable(MainDestinations.PAYMENT) {
            PaymentScreen(
                cartViewModel = cartViewModel,
                paymentViewModel = paymentViewModel,
                // --- 1. FIX: Correct lambda type ---
                onNavigateToReceipt = { orderId ->
                    navController.navigate("${MainDestinations.PAYMENT_SUCCESS_ROUTE}/$orderId") {
                        popUpTo(MainDestinations.PAYMENT) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = MainDestinations.PAYMENT_SUCCESS, // "payment_success/{orderId}"
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->

            val orderId = backStackEntry.arguments?.getInt("orderId")

            PaymentSuccessScreen(
                onNavigateToReceipt = {
                    // --- 4. FIX: Reset VM state HERE ---
                    paymentViewModel.resetPaymentState()
                    if (orderId != null) {
                        // --- 2. FIX: Navigate to the correct route ---
                        navController.navigate("${MainDestinations.RECEIPT_DETAIL_ROUTE}/$orderId?success=true") {
                            popUpTo(MainDestinations.PAYMENT_SUCCESS) { inclusive = true }
                        }
                    } else {
                        // Fallback
                        navController.navigate(MainDestinations.RECEIPTS) {
                            popUpTo(MainDestinations.PAYMENT_SUCCESS) { inclusive = true }
                        }
                    }
                }
            )
        }

        // --- 3. NEW: Add the Receipt Detail route ---
        composable(
            route = MainDestinations.RECEIPT_DETAIL,
            arguments = listOf(
                navArgument("orderId") { type = NavType.IntType },
                navArgument("success") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId")
            val showSuccess = backStackEntry.arguments?.getBoolean("success")

            if (orderId != null) {
                ReceiptDetailScreen(
                    orderId = orderId,
                    viewModel = receiptsViewModel,
                    showSuccessBanner = showSuccess ?: false
                )
            } else {
                Text("Error: Order ID missing.")
            }
        }
    }
}

// --- ROUTES OBJECT FOR THE MAIN APP ---
object MainDestinations {
    const val HOME = "home"
    const val MENU = "menu"
    const val CART = "cart"
    const val RECEIPTS = "receipts"
    const val PROFILE = "profile"
    const val PAYMENT = "payment"
    const val PAYMENT_SUCCESS_ROUTE = "payment_success"
    const val PAYMENT_SUCCESS = "payment_success/{orderId}"
    const val RECEIPT_DETAIL_ROUTE = "receipt_detail"
    // --- 4. FIX: Add the full route definition ---
    const val RECEIPT_DETAIL = "receipt_detail/{orderId}?success={success}"
}

// --- UPDATED BOTTOM NAVIGATION ---
@Composable
private fun CoffeeShopBottomNavigation(
    currentRoute: String?,
    onItemSelected: (String) -> Unit
) {
    val items = listOf(
        MainDestinations.HOME to (Icons.Filled.Home to "Home"),
        MainDestinations.MENU to (Icons.Filled.Menu to "Menu"),
        MainDestinations.CART to (Icons.Filled.ShoppingCart to "Cart"),
        MainDestinations.RECEIPTS to (Icons.Filled.Receipt to "Receipts"),
        MainDestinations.PROFILE to (Icons.Filled.Person to "Profile")
    )

    NavigationBar(containerColor = White) {
        items.forEach { (route, details) ->
            val (icon, label) = details
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = { onItemSelected(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CoffeeBrown,
                    selectedTextColor = CoffeeBrown,
                    indicatorColor = LightBrown,
                    unselectedIconColor = TextGrey,
                    unselectedTextColor = TextGrey
                )
            )
        }
    }
}

// --- HOME SCREEN CONTENT (for the "Home" tab) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    viewModel: HomeViewModel,
    cartViewModel: CartViewModel,
    showSnackbar: (String) -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val menuUiState by viewModel.menuUiState.collectAsState()
    val isRefreshing = menuUiState is MenuUiState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::fetchMenuItems,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            GreetingSection(userName = userName)
            Spacer(modifier = Modifier.height(32.dp))
            TodaySpecialSection(
                menuUiState = menuUiState,
                onAddItemClick = { item ->
                    cartViewModel.addToCart(item, "single") // <-- 6. FIX: Use "single" (lowercase)
                    showSnackbar("Added ${item.name} to cart")
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- GREETING SECTION ---
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

// --- TODAY'S SPECIAL SECTION ---
@Composable
private fun TodaySpecialSection(
    menuUiState: MenuUiState,
    onAddItemClick: (MenuItemUiModel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Today’s Special",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (menuUiState) {
            is MenuUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .size(50.dp),
                    color = CoffeeBrown,
                    strokeWidth = 4.dp
                )
            }
            is MenuUiState.Error -> {
                Text(
                    text = "Failed to load items. Please try again later.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is MenuUiState.Success -> {
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
                                    item = itemPair[0],
                                    onAddItemClick = { onAddItemClick(itemPair[0]) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (itemPair.size > 1) {
                                    SpecialItemCard(
                                        item = itemPair[1],
                                        onAddItemClick = { onAddItemClick(itemPair[1]) },
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
}

// --- SPECIAL ITEM CARD ---
@Composable
private fun SpecialItemCard(
    item: MenuItemUiModel,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("SpecialItemCard", "Loading image for ${item.name}: ${item.fullImageUrl}")
    Card(
        modifier = modifier.clickable(onClick = onAddItemClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBrown),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = item.fullImageUrl,
                contentDescription = item.name,
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
                        text = "KES ${item.singlePrice.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                    Text(
                        text = item.name,
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
                        .background(CoffeeBrown)
                        .clickable(onClick = onAddItemClick),
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