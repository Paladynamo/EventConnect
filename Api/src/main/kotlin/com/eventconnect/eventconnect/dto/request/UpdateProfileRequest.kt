package com.eventconnect.eventconnect.dto.request

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val name: String? = null,

    @field:Size(max = 100, message = "El nombre de la tienda no puede exceder 100 caracteres")
    val storeName: String? = null,

    val avatarUrl: String? = null
)

