package com.example.linkedininterno.data.model

data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val user: UserDto
)
