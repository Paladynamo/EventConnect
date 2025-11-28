package com.eventconnect.eventconnect.dto.response

import com.eventconnect.eventconnect.domain.enums.EventCategory
import java.time.LocalDateTime

data class EventResponse(
    val id: Long,
    val title: String,
    val description: String,
    val category: EventCategory,
    val date: LocalDateTime,
    val storeId: Long,
    val storeName: String,
    val region: String,
    val comuna: String,
    val addressStreet: String,
    val addressNumber: String,
    val imageUrls: List<String>,
    val hasCapacity: Boolean,
    val capacityTotal: Int?,
    val capacityAvailable: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

