package com.eventconnect.eventconnect.mapper

import com.eventconnect.eventconnect.domain.model.Reservation
import com.eventconnect.eventconnect.dto.response.ReservationResponse

object ReservationMapper {
    fun toResponse(reservation: Reservation): ReservationResponse {
        return ReservationResponse(
            id = reservation.id!!,
            eventId = reservation.eventId,
            userId = reservation.userId,
            userEmail = reservation.userEmail,
            userName = reservation.userName,
            status = reservation.status,
            reason = reservation.reason,
            createdAt = reservation.createdAt,
            updatedAt = reservation.updatedAt
        )
    }
}

