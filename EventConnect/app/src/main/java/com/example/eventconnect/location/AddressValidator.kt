package com.example.eventconnect.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Validador de direcciones simple: combina heurísticas rápidas + intentos con Geocoder.
 * Busca evitar falsos positivos manteniendo bajo costo.
 */
object AddressValidator {
    private val cache = ConcurrentHashMap<String, Boolean>()

    data class Result(val valid: Boolean?, val attempts: Int, val message: String)

    suspend fun validate(context: Context, street: String, number: String, comuna: String, region: String): Result {
        val line = fullLine(street, number, comuna, region)
        if (line.isBlank()) return Result(valid = null, attempts = 0, message = "Dirección vacía")
        cache[line]?.let { return Result(valid = it, attempts = 0, message = if (it) "Dirección previamente validada" else "Dirección previamente marcada como inválida") }

        // Heurísticas mínimas
        if (street.length < 4 || street.count { it.isLetter() } < 2) return Result(false, 0, "Nombre de calle demasiado corto o sin suficientes letras")
        if (number.isNotBlank() && number.toIntOrNull() == null) return Result(false, 0, "Número debe ser numérico")

        // Intentos con variaciones
        val variants = buildList {
            add(line)
            add("$street $number, $comuna, $region, Chile")
            if (number.isNotBlank()) add("$street $number, $region")
            add("$comuna, $region, $street $number")
        }.distinct()

        var success: Boolean? = null
        var attempts = 0
        for (v in variants) {
            val ok = geocode(context, v)
            attempts++
            if (ok == true) { success = true; break }
            if (ok == false && success != true) { success = false }
        }
        success?.let { cache[line] = it }
        val msg = when (success) {
            true -> "Dirección verificada"
            false -> "No se encontró en mapas"
            null -> "Sin conectividad o geocodificador no disponible"
        }
        return Result(success, attempts, msg)
    }

    private fun fullLine(street: String, number: String, comuna: String, region: String): String =
        listOf(street.trim(), number.trim(), comuna.trim(), region.trim(), "Chile").filter { it.isNotBlank() }.joinToString(", ")

    private suspend fun geocode(context: Context, query: String): Boolean? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("es", "CL"))
            return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API moderna requiere callback; simplificamos usando blocking fallback si disponible
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 1)?.let { it.isNotEmpty() }
            } else {
                @Suppress("DEPRECATION") val list = geocoder.getFromLocationName(query, 1)
                list != null && list.isNotEmpty()
            }
        } catch (_: Exception) { null }
    }
}

