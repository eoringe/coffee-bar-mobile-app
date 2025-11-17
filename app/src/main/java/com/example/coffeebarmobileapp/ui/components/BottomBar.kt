package com.example.coffeebarmobileapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coffeebarmobileapp.ui.theme.*
import androidx.compose.material.icons.filled.ArrowBack
import com.example.coffeebarmobileapp.ui.home.MainDestinations
import com.example.coffeebarmobileapp.ui.theme.Black
import com.example.coffeebarmobileapp.ui.theme.White
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color

/**
 * The main Bottom Navigation Bar for the app.
 */
@Composable
fun CoffeeShopBottomNavigation(
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

/**
 * The Top App Bar for the Home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeShopTopAppBar(
    unreadCount: Int = 0,
    onNotificationClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
        },
        actions = {
            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = CoffeeBrown
                    )
                }
                if (unreadCount > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = Color(0xFFE53935)
                    ) {
                        Text(
                            if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

/**
 * The Top App Bar for the Profile screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

/**
 * The Top App Bar for the Menu screen with expandable search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTopAppBar(
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                // Show search field when active
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search menu...", fontSize = 16.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearchActiveChange(false)
                            onSearchQueryChange("") // Clear search
                        }) {
                            Icon(Icons.Filled.Close, "Close search", tint = CoffeeBrown)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoffeeBrown,
                        unfocusedBorderColor = LightBrown
                    )
                )
            } else {
                // Show normal title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalCafe,
                        contentDescription = "Coffee",
                        modifier = Modifier.size(28.dp),
                        tint = CoffeeBrown
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Menu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = CoffeeBrown
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    )
}



/**
 * The Top App Bar for the Cart screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartTopAppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
                },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

/**
 * The Top App Bar for the Receipts screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsTopAppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

/**
 * --- ADD THIS NEW FUNCTION ---
 * The Top App Bar for the Receipt Detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailTopAppBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Receipt Details",
                fontWeight = FontWeight.Bold,
                color = Black
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}