package com.example.coffeebarmobileapp.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coffeebarmobileapp.data.models.User
import com.example.coffeebarmobileapp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.coffeebarmobileapp.data.remote.AuthApi
import android.util.Log

/**
 * ViewModel for authentication screens
 * Handles business logic and state management
 */
class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    private val authApi = AuthApi()

    fun checkBackendConnection() {
        viewModelScope.launch {
            val result = authApi.testConnection()
            Log.d("TEST", result.toString())
        }
    }

    /**
     * Sign up new user
     */
    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)

            val result = repository.signUp(email, password, name)

            result.fold(
                onSuccess = { user ->
                    _state.value = AuthState(
                        isSuccess = true,
                        user = user
                    )
                },
                onFailure = { error ->
                    _state.value = AuthState(
                        error = error.message ?: "Sign up failed"
                    )
                }
            )
        }
    }

    /**
     * Login existing user
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)

            val result = repository.login(email, password)

            result.fold(
                onSuccess = { user ->
                    _state.value = AuthState(
                        isSuccess = true,
                        user = user
                    )
                },
                onFailure = { error ->
                    _state.value = AuthState(
                        error = error.message ?: "Login failed"
                    )
                }
            )
        }
    }

    /**
     * Handle Google Sign-In
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)

            val result = repository.signInWithGoogle(idToken)

            result.fold(
                onSuccess = { user ->
                    _state.value = AuthState(
                        isSuccess = true,
                        user = user
                    )
                },
                onFailure = { error ->
                    _state.value = AuthState(
                        error = error.message ?: "Google Sign-In failed"
                    )
                }
            )
        }
    }


    /**
     * Logout user
     */
    fun logout() {
        repository.logout()
        _state.value = AuthState()
    }
}

/**
 * State for authentication screens
 */
data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

