package com.example.coffeebarmobileapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.coffeebarmobileapp.services.MyFirebaseMessagingService
import com.example.coffeebarmobileapp.ui.navigation.NavGraph
import com.example.coffeebarmobileapp.ui.theme.CoffeeBarMobileAppTheme
import com.google.firebase.FirebaseApp
import com.example.coffeebarmobileapp.ui.auth.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    // --- 1. Add a Tag for logging ---
    private val TAG = "MainActivityLifecycle"

    // Notification permission launcher for Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.w(TAG, "Notification permission denied - notifications won't work")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 2. Add your log ---
        Log.d(TAG, "onCreate: Activity is being created.")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()

        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Requesting notification permission...")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d(TAG, "Notification permission already granted")
            }
        }

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

    /**
     * Creates notification channel for Android 8.0+
     * This must be called before showing any notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MyFirebaseMessagingService.CHANNEL_ID,
                MyFirebaseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for order updates and payment confirmations"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}