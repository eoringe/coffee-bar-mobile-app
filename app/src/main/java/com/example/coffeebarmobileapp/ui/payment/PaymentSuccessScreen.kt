package com.example.coffeebarmobileapp.ui.payment



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Check

import androidx.compose.material3.Icon

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.coffeebarmobileapp.ui.theme.White

import kotlinx.coroutines.delay



@Composable

fun PaymentSuccessScreen(onNavigateToReceipt: () -> Unit) {



// This effect runs once when the screen appears

    LaunchedEffect(Unit) {

        delay(2500) // Wait for 2.5 seconds

        onNavigateToReceipt() // Navigate to the receipt

    }



    Scaffold(containerColor = White) { padding ->

        Box(

            modifier = Modifier

                .fillMaxSize()

                .padding(padding),

            contentAlignment = Alignment.Center

        ) {

            Column(

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center

            ) {

// Green Checkmark

                Box(

                    modifier = Modifier

                        .size(100.dp)

                        .clip(CircleShape)

                        .background(Color(0xFF00E676)), // Bright Green

                    contentAlignment = Alignment.Center

                ) {

                    Icon(

                        imageVector = Icons.Default.Check,

                        contentDescription = "Success",

                        tint = Color.White,

                        modifier = Modifier.size(60.dp)

                    )

                }

                Spacer(modifier = Modifier.height(24.dp))



// Text

                Text(

                    "Payment Successful!",

                    color = Color.Black,

                    fontSize = 22.sp,

                    fontWeight = FontWeight.Bold

                )

            }

        }

    }

}



@Preview

@Composable

private fun PaymentSuccessPreview() {

    PaymentSuccessScreen(onNavigateToReceipt = {})

}