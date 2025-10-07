package com.example.coffeebarmobileapp
//


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.coffeebarmobileapp.ui.navigation.NavGraph
import com.example.coffeebarmobileapp.ui.theme.CoffeeBarMobileAppTheme
import com.google.firebase.FirebaseApp
import com.example.coffeebarmobileapp.ui.auth.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels() // Get your ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            CoffeeBarMobileAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)

                    // Trigger backend connection test when the UI is composed
                    LaunchedEffect(Unit) {
                        authViewModel.checkBackendConnection()
                    }
                }
            }
        }
    }
}
