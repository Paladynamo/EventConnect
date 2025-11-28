package com.eventconnect.eventconnect.dto.response

import com.eventconnect.eventconnect.domain.enums.UserType
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val userType: UserType,
    val storeName: String?,
    val avatarUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

