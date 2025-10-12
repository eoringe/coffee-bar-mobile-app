package com.example.coffeebarmobileapp.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = viewModel.state.value

    // --- Google Sign-In Logic ---
    // IMPORTANT: Replace this with your Web Client ID from google-services.json
    val webClientId = "285872124510-c7bjb4darap8l60d2881utac2q2a0l2i.apps.googleusercontent.com"

    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Log.e("SignUpScreen", "Google Sign-In failed: ID token was null.")
                }
            } catch (e: ApiException) {
                Log.e("SignUpScreen", "Google Sign-In failed with ApiException", e)
            }
        }
    }
    // --- End of Google Sign-In Logic ---

    // Navigate on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSignUpSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign up button
        Button(
            onClick = {
                if (password == confirmPassword) {
                    scope.launch {
                        viewModel.signUp(email, password, name)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading &&
                    name.isNotBlank() &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    password == confirmPassword
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Sign up with Google")
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        TextButton(
            onClick = onNavigateToLogin,
            enabled = !state.isLoading
        ) {
            Text("Already have an account? Login")
        }

        // Error message
        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Password mismatch
        if (password.isNotBlank() && confirmPassword.isNotBlank() && password != confirmPassword) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
