package com.example.coffeebarmobileapp.ui.receipts



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.coffeebarmobileapp.ui.payment.Receipt

import com.example.coffeebarmobileapp.ui.payment.ReceiptItem

import com.example.coffeebarmobileapp.ui.theme.CoffeeBrown

import com.example.coffeebarmobileapp.ui.theme.LightBrown

import com.example.coffeebarmobileapp.ui.theme.TextGrey

import java.text.SimpleDateFormat

import java.util.Locale

import java.util.TimeZone



@Composable

fun ReceiptDetailScreen(

    orderId: Int, // <-- Accepts the ID

    viewModel: ReceiptsViewModel, // <-- Uses the ReceiptsViewModel

    showSuccessBanner: Boolean = false // <-- Can show "Payment Success!"

) {

    // Fetch the receipt when the screen is first shown

    LaunchedEffect(orderId) {

        viewModel.fetchReceiptById(orderId)

    }



    val uiState by viewModel.selectedReceiptState.collectAsState()



    Box(modifier = Modifier.fillMaxSize()) {

        when (val state = uiState) {

            is ReceiptDetailUiState.Loading -> {

                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            }

            is ReceiptDetailUiState.Error -> {

                Text(

                    text = state.message,

                    color = MaterialTheme.colorScheme.error,

                    modifier = Modifier.align(Alignment.Center)

                )

            }

            is ReceiptDetailUiState.Success -> {

                val receipt = state.receipt

                LazyColumn(

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(16.dp),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    // Show the "Payment Success" banner if we just paid

                    if (showSuccessBanner) {

                        item {

                            Icon(

                                imageVector = Icons.Filled.CheckCircle,

                                contentDescription = "Success",

                                tint = Color(0xFF00E676),

                                modifier = Modifier.size(80.dp)

                            )

                            Text(

                                "Payment Success!",

                                style = MaterialTheme.typography.headlineSmall,

                                fontWeight = FontWeight.Bold

                            )

                            Text("Your payment has been successfully done.")

                            Spacer(modifier = Modifier.height(32.dp))

                        }

                    }



                    item {

                        Text("Total Payment", fontSize = 16.sp)

                        Text(

                            "KES ${receipt.totalAmount.toInt()}",

                            fontSize = 28.sp,

                            fontWeight = FontWeight.Bold,

                            color = CoffeeBrown

                        )

                        Spacer(modifier = Modifier.height(32.dp))

                    }



                    item {

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                            ReceiptDetailRow(label = "Receipt No.", value = receipt.receiptNumber)

                            ReceiptDetailRow(

                                label = "Payment Time",

                                value = formatTimestamp(receipt.paymentDate)

                            )

                            ReceiptDetailRow(

                                label = "Phone Number",

                                value = receipt.customerPhoneNumber

                            )

                            receipt.mpesaReceiptNumber?.let {

                                ReceiptDetailRow(label = "M-Pesa Code", value = it)

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                    }



                    item {

                        Text(

                            "Items Purchased",

                            style = MaterialTheme.typography.titleMedium,

                            fontWeight = FontWeight.Bold,

                            modifier = Modifier.padding(horizontal = 16.dp)

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                    }



                    items(receipt.items) { item ->

                        ReceiptItemRow(item = item)

                    }



                    item {

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = LightBrown, thickness = 1.dp)

                            ReceiptTotalRow(label = "Subtotal", amount = receipt.subtotal)

                            ReceiptTotalRow(label = "Tax", amount = receipt.tax)

                            ReceiptTotalRow(

                                label = "Total Amount",

                                amount = receipt.totalAmount,

                                isTotal = true

                            )

                            Spacer(modifier = Modifier.height(32.dp))

                        }

                    }



                    // Removed the "View All Receipts" button

                }

            }

        }

    }

}



// ... (All the private helper functions like ReceiptDetailRow, ReceiptItemRow, etc.

// are exactly the same as before. You can copy them from your old file.)



// --- Helper Functions ---

@Composable

private fun ReceiptDetailRow(label: String, value: String) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 8.dp),

        horizontalArrangement = Arrangement.SpaceBetween

    ) {

        Text(label, color = TextGrey)

        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)

    }

}



@Composable

private fun ReceiptItemRow(item: ReceiptItem) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 16.dp, vertical = 4.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(

            text = "${item.quantity}x ${item.itemName} (${item.size.capitalize()})",

            style = MaterialTheme.typography.bodyMedium

        )

        Text(

            text = "KES ${item.lineTotal.toInt()}",

            style = MaterialTheme.typography.bodyMedium,

            fontWeight = FontWeight.SemiBold

        )

    }

}



@Composable

private fun ReceiptTotalRow(label: String, amount: Double, isTotal: Boolean = false) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 4.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(

            text = label,

            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,

            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,

            color = if (isTotal) Color.Black else TextGrey

        )

        Text(

            text = "KES ${amount.toInt()}",

            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,

            fontWeight = FontWeight.Bold

        )

    }

}



private fun formatTimestamp(timestamp: String): String {

    return try {

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {

            timeZone = TimeZone.getTimeZone("UTC") // Assume server sends UTC

        }

        val outputFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).apply {

            timeZone = TimeZone.getDefault() // Convert to local time

        }

        val date = inputFormat.parse(timestamp)

        outputFormat.format(date!!)

    } catch (e: Exception) {

        timestamp // Return original if parsing fails

    }

}



private fun String.capitalize(): String {

    return this.replaceFirstChar { char: Char ->

        if (char.isLowerCase()) char.titlecase() else char.toString()

    }

}

