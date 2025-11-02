package com.example.coffeebarmobileapp.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coffeebarmobileapp.ui.auth.LoginScreen
import com.example.coffeebarmobileapp.ui.auth.SignUpScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.SignUp.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) // clears backstack
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) // clears backstack
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Home.route) { HomeScreen() }
    }
}

@Composable
fun HomeScreen() {
    // Log Firebase user token when HomeScreen is displayed
    LaunchedEffect(Unit) {
        Log.d("NavGraph", "HomeScreen displayed - attempting to log Firebase token...")
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            Log.d(
                    "NavGraph",
                    "Current user check: ${if (user != null) "User found" else "User is NULL"}"
            )

            if (user != null) {
                Log.d("NavGraph", "Getting ID token for user: ${user.uid}")
                val tokenResult = user.getIdToken(false).await()
                val token = tokenResult.token

                Log.d("NavGraph", "=== FIREBASE USER TOKEN ===")
                Log.d("NavGraph", "User UID: ${user.uid}")
                Log.d("NavGraph", "User Email: ${user.email}")
                Log.d("NavGraph", "Token Length: ${token?.length ?: 0}")
                Log.d("NavGraph", "Token Preview (first 50 chars): ${token?.take(50)}...")
                Log.d("NavGraph", "Full Token: $token")
                Log.d("NavGraph", "=== END TOKEN ===")

                // Also log to INFO level for better visibility
                Log.i(
                        "NavGraph",
                        "🔥 TOKEN RETRIEVED SUCCESSFULLY - Check logs above for full token"
                )
            } else {
                Log.w("NavGraph", "⚠️ No Firebase user found - user is null")
                Log.w("NavGraph", "Make sure user has signed in before navigating to Home screen")
            }
        } catch (e: Exception) {
            Log.e("NavGraph", "❌ Failed to get Firebase token: ${e.message}", e)
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Welcome to Coffee Bar!")
    }
}
