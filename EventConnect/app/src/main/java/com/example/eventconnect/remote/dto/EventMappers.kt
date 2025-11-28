package com.example.eventconnect.remote.dto

import com.example.eventconnect.data.Event

/**
 * Funciones de mapeo para convertir entre DTOs de API y modelos Room
 */

/**
 * Convierte la categoría de la API al formato de la UI
 * Ahora la UI usa los mismos valores que el backend, así que no hay conversión
 * Categorías: "TCG", "JUEGOS_DE_MESA", "EVENTOS"
 */
private fun mapCategoryFromApi(apiCategory: String): String {
    // Ya no hay conversión, se usa el mismo formato que el backend
    return apiCategory
}

/**
 * Convierte la categoría de la UI al formato de la API
 * Categorías UI: "TCG", "Juegos de Mesa", "Eventos"
 * Categorías API: "TCG", "JUEGOS_DE_MESA", "EVENTOS"
 */
fun mapCategoryToApi(uiCategory: String): String {
    return when (uiCategory) {
        "Juegos de Mesa" -> "JUEGOS_DE_MESA"
        "Eventos" -> "EVENTOS"
        "TCG" -> "TCG"
        else -> uiCategory.uppercase() // Si no coincide, convertir a mayúsculas
    }
}

fun EventResponse.toEvent(): Event = Event(
    id = id.toInt(), // Convertir Long a Int para Room
    title = title,
    description = description,
    category = mapCategoryFromApi(category), // Convertir categoría de API a formato UI
    date = date, // Ya viene en formato ISO 8601
    store = storeName,
    region = region,
    comuna = comuna,
    addressStreet = addressStreet,
    addressNumber = addressNumber,
    imageUris = imageUrls,
    hasCapacity = hasCapacity,
    capacityTotal = capacityTotal,
    capacityAvailable = capacityAvailable
)

fun List<EventResponse>.toEvents(): List<Event> = map { it.toEvent() }

