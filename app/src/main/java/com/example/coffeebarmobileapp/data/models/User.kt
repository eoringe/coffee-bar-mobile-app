package com.example.coffeebarmobileapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val email: String?,
    val name: String?
)

@Serializable
data class ApiResponse(
    val message: String? = null,
    val error: String? = null
)
