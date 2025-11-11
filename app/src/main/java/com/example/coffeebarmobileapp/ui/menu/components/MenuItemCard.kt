package com.example.coffeebarmobileapp.ui.menu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MenuItemCard(
    itemName: String,
    price: Int,
    imageUrl: String? = null,
    onOrderClick: () -> Unit = {}, // Callback for Order button
    onAddToCartClick: () -> Unit = {} // Callback for Add to Cart button
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(120.dp) // Increased height for two buttons
            .border(
                width = 1.dp,
                color = Color(0xFF3A322C),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFCACA),
                        Color(0xFFFFFFFF)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image on the left
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = itemName,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no image
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Text and buttons column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Item name and price
                Column {
                    Text(
                        itemName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF3A322C)
                    )
                    Text(
                        "Ksh $price",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3A322C)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add to Cart button (outlined style)
                    OutlinedButton(
                        onClick = onAddToCartClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6F4E37)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        )
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Add to cart",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cart", style = MaterialTheme.typography.bodySmall)
                    }

                    // Order button (filled style)
                    Button(
                        onClick = onOrderClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6F4E37)
                        )
                    ) {
                        Text("Order", color = Color.White)
                    }
                }
            }
        }
    }
}
