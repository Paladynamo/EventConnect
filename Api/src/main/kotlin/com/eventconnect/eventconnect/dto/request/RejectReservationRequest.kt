package com.eventconnect.eventconnect.dto.request

import jakarta.validation.constraints.NotBlank

data class RejectReservationRequest(
    @field:NotBlank(message = "El motivo del rechazo es requerido")
    val reason: String
)

