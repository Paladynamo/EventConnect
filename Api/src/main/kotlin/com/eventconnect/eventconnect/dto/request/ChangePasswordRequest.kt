package com.eventconnect.eventconnect.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "La contraseña actual es requerida")
    val currentPassword: String,

    @field:NotBlank(message = "La nueva contraseña es requerida")
    @field:Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    val newPassword: String
)

