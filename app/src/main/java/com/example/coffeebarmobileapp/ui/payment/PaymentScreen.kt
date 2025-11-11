package com.example.coffeebarmobileapp.ui.payment

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffeebarmobileapp.R
import com.example.coffeebarmobileapp.ui.cart.CartItem
import com.example.coffeebarmobileapp.ui.cart.CartViewModel
import com.example.coffeebarmobileapp.ui.theme.*

@Composable
fun PaymentScreen(
    cartViewModel: CartViewModel,
    paymentViewModel: PaymentViewModel = viewModel(),
    onNavigateToReceipt: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val cartState by cartViewModel.uiState.collectAsState()
    val paymentState by paymentViewModel.uiState.collectAsState()

    val isLoading = paymentState is PaymentUiState.Loading

    var selectedOption by remember { mutableStateOf("primary") }
    var phoneNumber by remember { mutableStateOf("") }

    // --- State for Dialogs ---
    var showPendingDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    val total = cartState.subtotal

    LaunchedEffect(Unit) {
        paymentViewModel.resetPaymentState()
    }

    // This block observes the payment state
    LaunchedEffect(paymentState) {
        when (val state = paymentState) {
            is PaymentUiState.Success -> onNavigateToReceipt(state.orderId)
            is PaymentUiState.Pending -> {
                showPendingDialog = true
            }
            is PaymentUiState.Failed -> {
                showErrorDialog = state.error
            }
            else -> {}
        }
    }

    // --- Dialogs ---
    if (showPendingDialog) {
        PendingPaymentDialog(
            onDismiss = {
                showPendingDialog = false
                paymentViewModel.resetPaymentState()
                onNavigateBack() // Go back to cart
            }
        )
    }

    if (showErrorDialog != null) {
        ErrorPaymentDialog(
            error = showErrorDialog ?: "An unknown error occurred.",
            onDismiss = {
                showErrorDialog = null
                paymentViewModel.resetPaymentState()
            }
        )
    }

    // --- Main UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. "Pay with M-pesa" title ---
        PayWithMpesaTitle()

        Spacer(modifier = Modifier.height(32.dp))

        // --- 2. Payment Summary Card ---
        PaymentSummaryCard(
            cartItems = cartState.items,
            total = cartState.subtotal
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- 3. Phone Number ---
        Column(modifier = Modifier.fillMaxWidth()) {
            // Enter Number
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enter Phone Number")
            }
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                placeholder = { Text("e.g. 0722333444") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoffeeBrown, unfocusedBorderColor = LightBrown, focusedTextColor = TextGrey)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 4. Pay Button ---
        if (paymentState is PaymentUiState.Loading) {
            CircularProgressIndicator(color = CoffeeBrown)
            Text(
                "Processing...",
                color = CoffeeBrown,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Button(
                onClick = {
                    val numberToPay = phoneNumber
                    paymentViewModel.startPayment(cartViewModel, numberToPay)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("PAY NOW KES ${total.toInt()}", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PayWithMpesaTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Pay with",
            fontSize = 18.sp,
            color = TextGrey
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(id = R.drawable.mpesa_logo),
            contentDescription = "M-Pesa Logo",
            modifier = Modifier.height(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PaymentSummaryCard(cartItems: List<CartItem>, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))

            cartItems.forEach { cartItem ->
                SummaryItemRow(cartItem = cartItem) // <-- 3. Call new helper
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CoffeeBrown, thickness = 1.dp)
            SummaryRow(label = "TOTAL", amount = total, isTotal = true)
        }
    }
}

/**
 * Helper for displaying a single item in the summary
 */
@Composable
private fun SummaryItemRow(cartItem: CartItem) {
    val itemPrice = if (cartItem.selectedSize == "single") {
        cartItem.item.singlePrice
    } else {
        cartItem.item.doublePrice
    }
    val itemTotal = itemPrice * cartItem.quantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${cartItem.quantity}x ${cartItem.item.name} (${cartItem.selectedSize.capitalize()})",
            color = TextGrey,
            fontSize = 14.sp
        )
        Text(
            text = "KES ${itemTotal.toInt()}",
            color = TextGrey,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, isTotal: Boolean = false) {
    val fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
    val fontSize = if (isTotal) 20.sp else 16.sp
    val color = if (isTotal) Black else TextGrey

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = fontSize, fontWeight = fontWeight, color = color)
        Text(
            "KES ${amount.toInt()}",
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Black
        )
    }
}

@Composable
private fun PendingPaymentDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
        title = { Text("Payment Pending") },
        text = { Text("Your payment is processing. Please check your phone to confirm. We will notify you once it's complete.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ErrorPaymentDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Error", tint = Red) },
        title = { Text("Payment Failed") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Try Again")
            }
        }
    )
}