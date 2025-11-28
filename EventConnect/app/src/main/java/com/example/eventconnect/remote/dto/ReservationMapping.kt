package com.example.eventconnect.remote.dto

import com.example.eventconnect.data.Reservation
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Funciones de mapeo para convertir entre DTOs de API y modelos Room para Reservations
 */

fun ReservationResponse.toReservation(): Reservation = Reservation(
    id = id.toInt(), // Convertir Long a Int para Room
    eventId = eventId.toInt(),
    userEmail = userEmail,
    userName = userName,
    status = status,
    reason = reason,
    createdAt = parseIsoDateTimeToMillis(createdAt) ?: System.currentTimeMillis()
)

fun List<ReservationResponse>.toReservations(): List<Reservation> = map { it.toReservation() }

/**
 * Convierte una fecha ISO 8601 a timestamp en millis
 * Formato esperado: "2024-12-25T18:00:00" o "2024-12-25T18:00:00.000"
 * Compatible con API 24+
 */
private fun parseIsoDateTimeToMillis(isoDateTime: String): Long? {
    return try {
        // Intentar diferentes formatos ISO 8601
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(isoDateTime)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // Intentar siguiente formato
            }
        }
        
        // Si ninguno funciona, retornar null
        null
    } catch (e: Exception) {
        null
    }
}
