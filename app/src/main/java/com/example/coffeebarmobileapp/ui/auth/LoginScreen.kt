package com.example.coffeebarmobileapp.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coffeebarmobileapp.R
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

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val imageHeight = screenHeight * 0.35f // 35% of screen height

    val TAG = "LoginScreen"

    val gso = remember {
        val webClientId = "285872124510-c7bjb4darap8l60d2881utac2q2a0l2i.apps.googleusercontent.com"
        Log.d(TAG, "Configuring Google Sign-In with webClientId: $webClientId")

        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient: GoogleSignInClient = remember {
        Log.d(TAG, "Creating GoogleSignInClient")
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result received. ResultCode = ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Result OK, extracting sign-in account from intent.")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google account received: ${account?.email}")
                val idToken = account?.idToken
                if (idToken != null) {
                    Log.d(TAG, "Google ID Token successfully retrieved (length = ${idToken.length})")
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Log.e(TAG, "Google Sign-In failed: ID Token was null.")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed with ApiException. StatusCode=${e.statusCode}", e)
            }
        } else {
            Log.e(TAG, "Google Sign-In canceled or failed. ResultCode=${result.resultCode}")
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Log.d(TAG, "Login successful. Navigating to home screen.")
            onLoginSuccess()
        }
    }

    // UI
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6D3C7))
    ) {
        val screenWidth = maxWidth

        val imageHeight = screenHeight * 0.35f
        val cornerRadius = screenWidth * 0.45f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = screenWidth * 0.06f, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(bottomEnd = 160.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login),
                    contentDescription = "Login background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Coffee Bar Login",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF3A322C),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    Log.d(TAG, "Manual login clicked with email=$email")
                    scope.launch { viewModel.login(email, password) }
                },
                enabled = !state.isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 48.dp, max = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A322C)),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White
                    )
                } else {
                    Text("Login", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    Log.d(TAG, "Google Sign-In button clicked, launching sign-in intent.")
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(min = 48.dp, max = 56.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToSignUp, enabled = !state.isLoading) {
                Text("Don't have an account? Sign Up", color = Color(0xFF3A322C))
            }

            if (state.error != null) {
                Log.e(TAG, "Error occurred: ${state.error}")
                Text(state.error, color = MaterialTheme.colorScheme.error)
            } else if (state.user != null) {
                Log.d(TAG, "User authenticated successfully: ${state.user.email}")
                Text("Welcome ${state.user.email}!", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
