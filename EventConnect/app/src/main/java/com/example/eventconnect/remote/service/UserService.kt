package com.example.eventconnect.remote.service

import com.example.eventconnect.remote.dto.ChangePasswordRequest
import com.example.eventconnect.remote.dto.MessageResponse
import com.example.eventconnect.remote.dto.UpdateProfileRequest
import com.example.eventconnect.remote.dto.UserResponse
import retrofit2.http.*

interface UserService {
    @GET("users/profile")
    suspend fun getProfile(@Query("userId") userId: Long): UserResponse

    @PUT("users/profile")
    suspend fun updateProfile(
        @Query("userId") userId: Long,
        @Body request: UpdateProfileRequest
    ): UserResponse

    @PUT("users/password")
    suspend fun changePassword(
        @Query("userId") userId: Long,
        @Body request: ChangePasswordRequest
    ): MessageResponse

    @DELETE("users/account")
    suspend fun deleteAccount(@Query("userId") userId: Long): MessageResponse
}
