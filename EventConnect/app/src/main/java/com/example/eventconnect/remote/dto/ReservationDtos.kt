package com.example.eventconnect.remote.dto

/**
 * DTOs para reservas
 */

data class ReservationResponse(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val userEmail: String,
    val userName: String,
    val status: String, // "PENDING", "APPROVED", "REJECTED", "CANCELLED"
    val reason: String?,
    val createdAt: String, // ISO 8601 format
    val updatedAt: String // ISO 8601 format
)

data class CreateReservationRequest(
    val eventId: Long
)

data class RejectReservationRequest(
    val reason: String
)

