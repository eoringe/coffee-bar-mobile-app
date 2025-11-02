package com.example.coffeebarmobileapp.data.repository

import com.example.coffeebarmobileapp.data.models.User
import com.example.coffeebarmobileapp.data.remote.AuthApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ktx.userProfileChangeRequest

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
     * 2. SET THE USER'S DISPLAY NAME  <-- THIS IS THE NEW STEP
     * 3. Verify with backend
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            // Step 1: Create user in Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Failed to create user"))

            // --- STEP 2: SAVE THE USER'S NAME (THE FIX) ---
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdates).await()
            // ------------------------------------------------

            // Step 3: Get Firebase token
            val token = firebaseUser.getIdToken(false).await().token
                ?: return Result.failure(Exception("Failed to get token"))

            // Step 4: Verify with backend
            // This will now return a User object that includes the name
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
     * Sign in with Google
     * 1. Get credential from Google ID Token
     * 2. Sign in to Firebase with the credential
     * 3. Verify with backend
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            // Step 1: Create a Firebase credential from the Google ID token
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // Step 2: Sign in to Firebase with the credential
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Google Sign-In failed with Firebase"))

            // Step 3: Get Firebase token to send to your backend
            val token = firebaseUser.getIdToken(false).await().token
                ?: return Result.failure(Exception("Failed to get Firebase token after Google Sign-In"))

            // Step 4: Verify with your Ktor backend
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

    /**
     * Get Firebase ID token using callback pattern (matches Java example)
     * This is the equivalent of:
     * FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
     * mUser.getIdToken(true).addOnCompleteListener(...)
     */
    fun getCurrentUserToken(
        forceRefresh: Boolean = false,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val user: FirebaseUser? = firebaseAuth.currentUser
        
        if (user == null) {
            onError(Exception("No user is currently signed in"))
            return
        }

        user.getIdToken(forceRefresh)
            .addOnCompleteListener(object : OnCompleteListener<GetTokenResult> {
                override fun onComplete(task: Task<GetTokenResult>) {
                    if (task.isSuccessful) {
                        val idToken: String? = task.result?.token
                        if (idToken != null) {
                            // Send token to your backend via HTTPS
                            onSuccess(idToken)
                        } else {
                            onError(Exception("Token is null"))
                        }
                    } else {
                        // Handle error -> task.getException()
                        val exception = task.exception
                            ?: Exception("Unknown error getting token")
                        onError(exception as? Exception ?: Exception(exception.message))
                    }
                }
            })
    }
}

