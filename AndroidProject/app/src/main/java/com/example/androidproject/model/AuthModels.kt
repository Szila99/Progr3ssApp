package com.example.androidproject.model

data class User(
    val id: String,
    val email: String,
    val name: String
)
data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null
)
data class Tokens(
    val accessToken: String,
    val refreshToken: String
)
data class AuthResponse(
    val tokens: Tokens,
    val user: User
)
data class RefreshRequest(
    val refreshToken: String
)
data class SignUpRequest(
    val username: String,
    val email: String,
    val password: String
)
data class ResetPasswordRequest(
    val email: String
)
data class ResetPasswordResponse(
    val message: String
)