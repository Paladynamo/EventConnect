package com.eventconnect.eventconnect.dto.response

import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val store: String,
    val changes: String,
    val read: Boolean,
    val createdAt: LocalDateTime
)

