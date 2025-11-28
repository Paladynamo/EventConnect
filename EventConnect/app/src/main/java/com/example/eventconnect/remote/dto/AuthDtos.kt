package com.example.eventconnect.remote.dto

/**
 * DTOs para autenticación
 */

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val userType: String, // "USUARIO" o "TIENDA"
    val storeName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val userType: String, // "USUARIO" o "TIENDA"
    val storeName: String?,
    val avatarUrl: String?,
    val createdAt: String, // ISO 8601 format
    val updatedAt: String // ISO 8601 format
)

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

