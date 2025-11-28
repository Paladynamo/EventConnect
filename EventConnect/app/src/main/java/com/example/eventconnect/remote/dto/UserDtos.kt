package com.example.eventconnect.remote.dto

/**
 * DTOs para usuarios
 */

data class UpdateProfileRequest(
    val name: String? = null,
    val storeName: String? = null,
    val avatarUrl: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

