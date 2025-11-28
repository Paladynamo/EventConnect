package com.example.eventconnect.remote.service

import com.example.eventconnect.remote.dto.AuthResponse
import com.example.eventconnect.remote.dto.ForgotPasswordRequest
import com.example.eventconnect.remote.dto.LoginRequest
import com.example.eventconnect.remote.dto.MessageResponse
import com.example.eventconnect.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse
}
