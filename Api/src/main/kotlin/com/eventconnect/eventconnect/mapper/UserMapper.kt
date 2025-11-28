package com.eventconnect.eventconnect.mapper

import com.eventconnect.eventconnect.domain.model.User
import com.eventconnect.eventconnect.dto.response.UserResponse

object UserMapper {
    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            name = user.name,
            email = user.email,
            userType = user.userType,
            storeName = user.storeName,
            avatarUrl = user.avatarUrl,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}

