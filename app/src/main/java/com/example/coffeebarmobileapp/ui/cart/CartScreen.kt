package com.example.coffeebarmobileapp.ui.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.coffeebarmobileapp.ui.theme.*

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onCheckoutClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your cart is empty.", style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.items) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onRemoveClick = {
                            viewModel.removeFromCart(cartItem.item.id, cartItem.selectedSize)
                        },
                        onQuantityChange = { change ->
                            viewModel.updateQuantity(cartItem.item.id, cartItem.selectedSize, change)
                        },
                        onSizeChange = { newSize ->
                            viewModel.updateItemSize(cartItem.item.id, cartItem.selectedSize, newSize)
                        }
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightBrown),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sub Total", fontSize = 18.sp, color = TextGrey)
                        Text("KES ${uiState.subtotal.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("KES ${uiState.subtotal.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCheckoutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CHECKOUT", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartItemCard(
    cartItem: CartItem,
    onRemoveClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onSizeChange: (String) -> Unit
) {
    val price = if (cartItem.selectedSize == "single") {
        cartItem.item.singlePrice
    } else {
        cartItem.item.doublePrice
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(cartItem.item.fullImageUrl),
            contentDescription = cartItem.item.name,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cartItem.item.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("KES ${price.toInt()}", fontSize = 16.sp, color = TextGrey)
            Text(
                "Natural chilled caffeine-free blend",
                fontSize = 12.sp,
                color = TextGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = 8.dp)) {
                SegmentedButton(
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                    onClick = { onSizeChange("single") },
                    selected = cartItem.selectedSize == "single",
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CoffeeBrown,
                        activeContentColor = White,
                        inactiveContainerColor = LightBrown
                    )
                ) {
                    Text("Single", fontSize = 12.sp)
                }
                SegmentedButton(
                    shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                    onClick = { onSizeChange("double") },
                    selected = cartItem.selectedSize == "double",
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CoffeeBrown,
                        activeContentColor = White,
                        inactiveContainerColor = LightBrown
                    )
                ) {
                    Text("Double", fontSize = 12.sp)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(1.dp, LightBrown, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { onQuantityChange(-1) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = CoffeeBrown)
                }
                Text(cartItem.quantity.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { onQuantityChange(1) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = CoffeeBrown)
                }
            }
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Red)
            }
        }
    }
}