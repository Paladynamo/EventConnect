package com.eventconnect.eventconnect.dto.request

import com.eventconnect.eventconnect.domain.enums.UserType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "El nombre es requerido")
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val name: String,

    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String,

    @field:NotBlank(message = "La contraseña es requerida")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String,

    @field:NotNull(message = "El tipo de usuario es requerido")
    val userType: UserType,

    @field:Size(max = 100, message = "El nombre de la tienda no puede exceder 100 caracteres")
    val storeName: String? = null
)

