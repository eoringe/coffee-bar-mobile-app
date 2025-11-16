package com.example.coffeebarmobileapp.ui.receipts

import android.util.Log // <-- 1. ADD THIS IMPORT
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
import com.example.coffeebarmobileapp.ui.payment.Receipt
import com.example.coffeebarmobileapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.clickable

// --- 2. ADD A TAG FOR FILTERING LOGCAT ---
//private const val TAG = "ReceiptsScreen"

@Composable
fun ReceiptsScreen(
    viewModel: ReceiptsViewModel = viewModel(),
    onReceiptClick: (Int) -> Unit
) {
    Log.d("ReceiptsScreen", "ReceiptsScreen composable is active.")

    val uiState by viewModel.uiState.collectAsState()
    Log.d("ReceiptsScreen", "Current uiState is: ${uiState::class.simpleName}")

    LaunchedEffect(Unit) {
        Log.d("ReceiptsScreen", "LaunchedEffect: Calling viewModel.fetchReceipts()")
        viewModel.fetchReceipts()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ReceiptsUiState.Loading -> {
                Log.d("ReceiptsScreen", "State is Loading.")
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ReceiptsUiState.Error -> {
                // --- 3. THIS IS THE MOST IMPORTANT LOG ---
                // This will print the exact error message from your screenshot
                Log.e("ReceiptsScreen", "State is Error. Message: ${state.message}")
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
                Log.d("ReceiptsScreen", "State is Success. Receipt count: ${state.receipts.size}")
                if (state.receipts.isEmpty()) {
                    Log.d("ReceiptsScreen", "Receipts list is empty. Showing 'no past receipts' message.")
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
                                onClick = { onReceiptClick(receipt.orderId) }
                            )
                        }
                    }
                }
            }
            // --- 4. ADD AN ELSE BRANCH TO MAKE 'when' EXHAUSTIVE ---
            else -> {
                Log.w("ReceiptsScreen", "State is in an unexpected 'else' branch. This might be an issue.")
                Text("Unknown state.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ReceiptCard(receipt: Receipt, onClick: () -> Unit) {
    // --- 5. LOG WHEN A CARD IS RENDERED ---
    Log.d("ReceiptsScreen", "Rendering ReceiptCard for Order ID: ${receipt.orderId}, Ref: ${receipt.receiptNumber}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // <-- Make the whole card clickable
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightBrown.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total: KES ${receipt.totalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatSimpleDate(receipt.paymentDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGrey
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ref: ${receipt.receiptNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey
            )
            Text(
                "Items: ${receipt.items.joinToString { it.itemName }}",
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatSimpleDate(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        Log.w("ReceiptsScreen", "Failed to parse date: $timestamp")
        timestamp.split("T").firstOrNull() ?: timestamp
    }
}