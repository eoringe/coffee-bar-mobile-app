package com.example.coffeebarmobileapp.ui.receipts



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.coffeebarmobileapp.ui.payment.Receipt // <-- Import the new Receipt class

import com.example.coffeebarmobileapp.ui.theme.*

import java.text.SimpleDateFormat

import java.util.Locale

import java.util.TimeZone

import androidx.compose.foundation.clickable



@Composable

fun ReceiptsScreen(

    viewModel: ReceiptsViewModel = viewModel(),

    onReceiptClick: (Int) -> Unit

) {

    val uiState by viewModel.uiState.collectAsState()



    LaunchedEffect(Unit) {

        viewModel.fetchReceipts()

    }



    Box(modifier = Modifier.fillMaxSize()) {

        when (val state = uiState) {

            is ReceiptsUiState.Loading -> {

                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            }

            is ReceiptsUiState.Error -> {

                Text(

                    text = state.message,

                    color = MaterialTheme.colorScheme.error,

                    textAlign = TextAlign.Center,

                    modifier = Modifier

                        .align(Alignment.Center)

                        .padding(16.dp)

                )

            }

            is ReceiptsUiState.Success -> {

                if (state.receipts.isEmpty()) {

                    Text(

                        text = "You have no past receipts.",

                        style = MaterialTheme.typography.bodyLarge,

                        color = TextGrey,

                        modifier = Modifier.align(Alignment.Center)

                    )

                } else {

                    LazyColumn(

                        contentPadding = PaddingValues(16.dp),

                        verticalArrangement = Arrangement.spacedBy(16.dp)

                    ) {

                        items(state.receipts) { receipt ->

                            ReceiptCard(

                                receipt = receipt,

                                onClick = { onReceiptClick(receipt.orderId) } // <-- Add this line

                            )

                        }

                    }

                }

            }

        }

    }

}



@Composable

private fun ReceiptCard(receipt: Receipt, onClick: () -> Unit) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(12.dp),

        colors = CardDefaults.cardColors(containerColor = LightBrown.copy(alpha = 0.5f)),

        elevation = CardDefaults.cardElevation(0.dp)

    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(

                modifier = Modifier.fillMaxWidth()

                    .clickable(onClick = onClick),

                horizontalArrangement = Arrangement.SpaceBetween

            ) {

                Text(

                    // --- 1. USE THE NEW FIELD ---

                    "Total: KES ${receipt.totalAmount.toInt()}",

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold

                )

                Text(

                    // --- 2. USE THE NEW FIELD + FORMATTER ---

                    formatSimpleDate(receipt.paymentDate),

                    style = MaterialTheme.typography.bodyMedium,

                    color = TextGrey

                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(

                // --- 3. USE THE NEW FIELD ---

                "Ref: ${receipt.receiptNumber}",

                style = MaterialTheme.typography.bodySmall,

                color = TextGrey

            )

            Text(

                "Items: ${receipt.items.joinToString { it.itemName }}", // <-- Format the list

                style = MaterialTheme.typography.bodySmall,

                color = TextGrey,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis

            )

        }

    }

}



// --- 4. ADD THIS HELPER FUNCTION ---

private fun formatSimpleDate(timestamp: String): String {

    return try {

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {

            timeZone = TimeZone.getTimeZone("UTC")

        }

        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val date = inputFormat.parse(timestamp)

        outputFormat.format(date!!)

    } catch (e: Exception) {

        timestamp.split("T").firstOrNull() ?: timestamp

    }

}