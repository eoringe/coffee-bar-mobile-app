package com.example.coffeebarmobileapp.data.repository

import com.example.coffeebarmobileapp.data.models.User
import com.example.coffeebarmobileapp.data.remote.AuthApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository that handles authentication logic
 * Separates Firebase auth from backend verification
 */
class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val authApi = AuthApi()

    /**
     * Sign up with email and password
     * 1. Create user in Firebase
     * 2. Verify with backend
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            // Step 1: Create user in Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Failed to create user"))

            // Step 2: Get Firebase token
            val token = firebaseUser.getIdToken(false).await().token
                ?: return Result.failure(Exception("Failed to get token"))

            // Step 3: Verify with backend
            authApi.verifyUser(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with email and password
     * 1. Sign in with Firebase
     * 2. Verify with backend
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Step 1: Sign in with Firebase
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Failed to sign in"))

            // Step 2: Get Firebase token
            val token = firebaseUser.getIdToken(false).await().token
                ?: return Result.failure(Exception("Failed to get token"))

            // Step 3: Verify with backend
            authApi.verifyUser(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentUser() = firebaseAuth.currentUser
}