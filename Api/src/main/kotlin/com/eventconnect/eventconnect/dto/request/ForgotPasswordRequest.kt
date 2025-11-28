package com.eventconnect.eventconnect.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ForgotPasswordRequest(
    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String
)

