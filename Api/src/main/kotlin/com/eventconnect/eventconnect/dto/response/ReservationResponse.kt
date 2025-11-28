package com.eventconnect.eventconnect.dto.response

import com.eventconnect.eventconnect.domain.enums.ReservationStatus
import java.time.LocalDateTime

data class ReservationResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val userEmail: String,
    val userName: String,
    val status: ReservationStatus,
    val reason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

