package com.example.eventconnect.remote.dto

/**
 * DTOs comunes compartidos entre servicios
 */

data class MessageResponse(
    val message: String
)

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val last: Boolean
)

