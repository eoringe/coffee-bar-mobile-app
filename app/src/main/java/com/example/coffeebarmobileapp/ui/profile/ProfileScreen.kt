package com.example.coffeebarmobileapp.ui.profile

import android.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coffeebarmobileapp.ui.theme.*

/**
 * This is the TopAppBar for the Profile screen.
 * It has a centered logo and icons on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Coffee Bar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
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
 * This is the main Profile screen content.
 */
@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    isLoading: Boolean,
    onLogoutClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onSaveName: (newName: String, onSaveComplete: () -> Unit) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var nameFieldState by remember(userName) { mutableStateOf(userName) }

    // Reset field text if the viewmodel's name changes (e.g., after save)
    LaunchedEffect(userName) {
        if (!isEditing) {
            nameFieldState = userName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            TextButton(onClick = {
                isEditing = !isEditing
                nameFieldState = userName // Reset text if user cancels
            },
                enabled = !isLoading
            ) {
                Text(if (isEditing) "Cancel" else "Edit", fontSize = 16.sp, color = Black)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Black)
            }
        }
        // ------------------------------------

        Spacer(modifier = Modifier.height(24.dp))
        UserInfoBox(
            isEditing = isEditing,
            userName = nameFieldState,
            userEmail = userEmail,
            onNameChange = { nameFieldState = it },
            isLoading = isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (isEditing) {
            ProfileButton(
                text = if (isLoading) "Saving..." else "Save Changes",
                onClick = {
                    // 6. Call the new onSaveName function
                    onSaveName(nameFieldState) {
                        // This part runs ONLY when the save is complete
                        isEditing = false
                    }
                },
                enabled = !isLoading // 7. Disable while loading
            )
        } else {
            // --- Show Normal Buttons ---
            Spacer(modifier = Modifier.height(16.dp))
            ProfileButton(
                text = "Logout",
                onClick = onLogoutClick,
                contentColor = Color.White,
                containerColor = Red
            )
        }
    }
}

@Composable
fun UserInfoBox(
    isEditing: Boolean,
    userName: String,
    userEmail: String,
    onNameChange: (String) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CoffeeBrown, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Name",
                    tint = Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            }
            if (isEditing) {
                // Show TextField when editing
                OutlinedTextField(
                    value = userName,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextGrey, // <-- Use your dark grey color
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.width(200.dp), // Constrain width
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoffeeBrown,
                        unfocusedBorderColor = TextGrey.copy(alpha = 0.5f)
                    )
                )
            } else {
                // Show Text when not editing
                Text(userName, style = MaterialTheme.typography.bodyLarge, color = Black)
            }
        }
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "Email",
                    tint = Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Black)
            }
            Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = TextGrey)
        }
    }
}

@Composable
fun ProfileButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = LightBrown,
    contentColor: Color = Black,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            fontSize = 16.sp
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    Scaffold(
        topBar = { ProfileTopAppBar() }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ProfileScreen(
                userName = "Gift Gichuhi",
                userEmail = "gift@strathmore.edu",
                isLoading = false,
                onLogoutClick = {},
                onChangePasswordClick = {},
                onSaveName = { _, _ -> }            )
        }
    }
}