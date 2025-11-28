package com.eventconnect.eventconnect.dto.request

import jakarta.validation.constraints.NotNull

data class CreateReservationRequest(
    @field:NotNull(message = "El ID del evento es requerido")
    val eventId: Long
)

