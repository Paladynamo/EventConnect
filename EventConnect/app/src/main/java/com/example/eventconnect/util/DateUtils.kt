package com.example.eventconnect.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Utilidades de fecha reutilizables entre pantallas.
// dateToMillis: intenta parsear la fecha en varios formatos comunes y retornar milisegundos.
fun dateToMillis(dateStr: String): Long? {
    val patterns = listOf(
        "d/M/yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd"
    )
    for (p in patterns) {
        val sdf = SimpleDateFormat(p, Locale.getDefault())
        sdf.isLenient = false
        try {
            val d = sdf.parse(dateStr) ?: continue
            return d.time
        } catch (_: ParseException) { }
    }
    return null
}

// startOfToday: inicio del día actual en milisegundos para comparaciones (00:00 local).
val startOfToday: Long
    get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
