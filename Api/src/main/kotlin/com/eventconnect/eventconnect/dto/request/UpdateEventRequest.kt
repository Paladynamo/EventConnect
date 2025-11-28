package com.eventconnect.eventconnect.dto.request

import com.eventconnect.eventconnect.domain.enums.EventCategory
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class UpdateEventRequest(
    @field:Size(max = 200, message = "El título no puede exceder 200 caracteres")
    val title: String? = null,

    val description: String? = null,

    val category: EventCategory? = null,

    val date: LocalDateTime? = null,

    val region: String? = null,

    val comuna: String? = null,

    val addressStreet: String? = null,

    val addressNumber: String? = null,

    val imageUrls: List<String>? = null,

    val hasCapacity: Boolean? = null,

    val capacityTotal: Int? = null,

    val capacityAvailable: Int? = null
)

