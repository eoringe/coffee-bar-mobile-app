package com.example.coffeebarmobileapp

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

    private val authViewModel: AuthViewModel by viewModels()

    // --- 1. Add a Tag for logging ---
    private val TAG = "MainActivityLifecycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 2. Add your log ---
        Log.d(TAG, "onCreate: Activity is being created.")

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

    // --- 3. Add all the other lifecycle override functions ---
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Activity is becoming visible.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity is in the foreground and interactive.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Activity is partially obscured (e.g., dialog, split-screen).")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Activity is no longer visible (in background).")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Activity is being destroyed.")
    }
}