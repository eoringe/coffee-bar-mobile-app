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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val state = viewModel.state.value

    // 1. Configure Google Sign-In options
    val gso = remember {
        // Find your Web Client ID in the google-services.json file
        // It's the "client_id" inside the "client" array where "client_type" is 3
        val webClientId = "285872124510-c7bjb4darap8l60d2881utac2q2a0l2i.apps.googleusercontent.com"
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient: GoogleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // 2. Set up Activity Result Launcher
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
                    // Handle error: ID token is null
                    Log.e("LoginScreen", "Google Sign-In failed: ID Token was null.")
                }
            } catch (e: ApiException) {
                // Handle error: Google Sign-In failed
                Log.e("LoginScreen", "Google Sign-In failed with ApiException: ${e.statusCode}")
            }
        }
    }


    // Navigate on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
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
            text = "Coffee Bar Login",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

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

        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = {
                scope.launch {
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        OutlinedButton(
            onClick = {
                // 3. Launch the Google Sign-In flow
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            // You can add a Google icon here
            Text("Sign in with Google")
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Sign up button
        TextButton(
            onClick = onNavigateToSignUp,
            enabled = !state.isLoading
        ) {
            Text("Don't have an account? Sign Up")
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

        // Success message
        if (state.user != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome ${state.user.email}!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

