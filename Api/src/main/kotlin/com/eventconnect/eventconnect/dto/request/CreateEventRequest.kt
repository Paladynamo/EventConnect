package com.eventconnect.eventconnect.dto.request

import com.eventconnect.eventconnect.domain.enums.EventCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateEventRequest(
    @field:NotBlank(message = "El título es requerido")
    @field:Size(max = 200, message = "El título no puede exceder 200 caracteres")
    val title: String,

    @field:NotBlank(message = "La descripción es requerida")
    val description: String,

    @field:NotNull(message = "La categoría es requerida")
    val category: EventCategory,

    @field:NotNull(message = "La fecha es requerida")
    val date: LocalDateTime,

    @field:NotBlank(message = "La región es requerida")
    val region: String,

    @field:NotBlank(message = "La comuna es requerida")
    val comuna: String,

    @field:NotBlank(message = "La calle es requerida")
    val addressStreet: String,

    @field:NotBlank(message = "El número de dirección es requerido")
    val addressNumber: String,

    val imageUrls: List<String> = emptyList(),

    val hasCapacity: Boolean = false,

    val capacityTotal: Int? = null
)

