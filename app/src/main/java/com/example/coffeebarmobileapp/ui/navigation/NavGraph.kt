package com.example.coffeebarmobileapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coffeebarmobileapp.ui.auth.LoginScreen
import com.example.coffeebarmobileapp.ui.auth.SignUpScreen
import com.example.coffeebarmobileapp.ui.home.HomeScreen
import com.example.coffeebarmobileapp.ui.landing.LandingScreen
import com.example.coffeebarmobileapp.ui.navigation.Screen.SignUp

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        composable(Screen.Landing.route){
            LandingScreen( onNavigateToLogin = {
                navController.navigate(Screen.Login.route)
            },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) // clears backstack
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(SignUp.route)
                }
            )
        }

        composable(SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) // clears backstack
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                    onNavigateToLogin = { // <-- 1. Provide the lambda
                        navController.navigate(Screen.Login.route) { // 2. Navigate to Login
                            popUpTo(navController.graph.id) { // 3. Clear the entire app's back stack
                                inclusive = true
                            }
                        }
                    }

            )
        }
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Welcome to Coffee Bar!")
    }
}