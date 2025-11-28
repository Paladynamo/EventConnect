package com.example.eventconnect.remote.service

import com.example.eventconnect.remote.dto.MessageResponse
import com.example.eventconnect.remote.dto.NotificationResponse
import retrofit2.http.*

interface NotificationService {
    @GET("notifications")
    suspend fun getNotifications(@Query("userId") userId: Long): List<NotificationResponse>

    @PUT("notifications/mark-read")
    suspend fun markAsRead(@Query("userId") userId: Long): MessageResponse
}

