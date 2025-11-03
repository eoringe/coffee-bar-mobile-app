package com.example.coffeebarmobileapp.ui.menu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MenuItemCard(
    itemName: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(90.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF3A322C), // dark outline
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(itemName, style = MaterialTheme.typography.titleMedium, color = Color(0xFF3A322C))
                Text("Ksh 200", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3A322C))
            }

            Button(
                onClick = {},
                enabled = true,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Order")
            }
        }
    }
}
