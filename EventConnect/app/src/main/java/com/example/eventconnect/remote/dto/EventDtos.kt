package com.example.eventconnect.remote.dto

/**
 * DTOs para eventos
 */

data class EventResponse(
    val id: Long,
    val title: String,
    val description: String,
    val category: String, // "TCG", "JUEGOS_DE_MESA", "EVENTOS"
    val date: String, // ISO 8601 format: YYYY-MM-DDTHH:mm:ss
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
    val createdAt: String, // ISO 8601 format
    val updatedAt: String // ISO 8601 format
)

data class CreateEventRequest(
    val title: String,
    val description: String,
    val category: String, // "TCG", "JUEGOS_DE_MESA", "EVENTOS"
    val date: String, // ISO 8601 format: YYYY-MM-DDTHH:mm:ss
    val region: String,
    val comuna: String,
    val addressStreet: String,
    val addressNumber: String,
    val imageUrls: List<String> = emptyList(),
    val hasCapacity: Boolean = false,
    val capacityTotal: Int? = null
)

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val date: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val imageUrls: List<String>? = null,
    val hasCapacity: Boolean? = null,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null
)

