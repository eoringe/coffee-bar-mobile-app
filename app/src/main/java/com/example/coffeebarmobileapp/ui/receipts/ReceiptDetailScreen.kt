package com.example.coffeebarmobileapp.ui.receipts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.coffeebarmobileapp.ui.payment.ReceiptItem
import com.example.coffeebarmobileapp.ui.theme.CoffeeBrown
import com.example.coffeebarmobileapp.ui.theme.TextGrey
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    orderId: Int,
    viewModel: ReceiptsViewModel, // <-- Uses the ReceiptsViewModel
) {
    // Fetch the receipt when the screen is first shown
    LaunchedEffect(orderId) {
        viewModel.fetchReceiptById(orderId)
    }

    val uiState by viewModel.selectedReceiptState.collectAsState()
    val isRefreshing = uiState is ReceiptDetailUiState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchReceiptById(orderId) },
        modifier = Modifier.fillMaxSize()
    ) {
        // This LazyColumn is the scrollable content
        // We use the 'when(state)' *inside* the LazyColumn to build the UI
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is ReceiptDetailUiState.Loading -> {
                    // Don't show a second spinner, just the pull-to-refresh one
                    item {
                        Spacer(modifier = Modifier.height(200.dp))
                        Text(
                            "Loading Receipt...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextGrey
                        )
                    }
                }
                is ReceiptDetailUiState.Error -> {
                    item {
                        Spacer(modifier = Modifier.height(200.dp))
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is ReceiptDetailUiState.Success -> {
                    val receipt = state.receipt

                    item {
                        Text("Total Payment", fontSize = 20.sp, color = TextGrey)
                        Text(
                            "KES ${receipt.totalAmount}",
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
                            // This will now update when you pull to refresh
                            receipt.mpesaReceiptNumber?.let { mpesaCode ->
                                ReceiptDetailRow(label = "M-Pesa Transaction", value = mpesaCode)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Text(
                            "Items Purchased",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGrey,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(receipt.items) { item ->
                        ReceiptItemRow(item = item)
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = CoffeeBrown, thickness = 1.dp)
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
                }
            }
        }
    }
}

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
            timeZone = TimeZone.getTimeZone("EAT") // Convert to local time
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