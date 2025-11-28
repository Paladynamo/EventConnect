package com.example.eventconnect.remote.dto

/**
 * DTOs para notificaciones
 */

data class NotificationResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val store: String,
    val changes: String,
    val read: Boolean,
    val createdAt: String // ISO 8601 format
)

