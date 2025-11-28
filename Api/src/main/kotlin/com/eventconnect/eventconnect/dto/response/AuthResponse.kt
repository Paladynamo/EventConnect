package com.eventconnect.eventconnect.dto.response

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

