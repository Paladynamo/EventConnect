package com.example.eventconnect.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.eventconnect.data.Event
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarUtils {
    /**
     * Abre el calendario del sistema con un formulario de nuevo evento pre-rellenado
     * con los datos del evento. Evita pedir permisos de calendario usando ACTION_INSERT.
     * Devuelve true si se pudo lanzar la actividad.
     */
    fun addEventToCalendar(context: Context, event: Event): Boolean {
        val title = event.title
        val description = buildString {
            append(event.description)
            if (event.store.isNotBlank()) {
                append("\nOrganiza: ").append(event.store)
            }
        }
        val location = listOf(event.addressStreet, event.addressNumber, event.comuna, event.region)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        val times = parseDateRange(event.date)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            if (location.isNotBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            if (times != null) {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, times.first)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, times.second)
            }
            // Sugerencias: alerta por defecto
            putExtra(CalendarContract.Reminders.MINUTES, 30)
            putExtra(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    // Intenta parsear varias formas comunes de fecha. Si sólo hay día, asume 09:00-10:00 local.
    private fun parseDateRange(dateStr: String?): Pair<Long, Long>? {
        if (dateStr.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
        )
        var parsed: Date? = null
        var hasTime = false
        for (p in patterns) {
            val sdf = SimpleDateFormat(p, Locale.getDefault())
            try {
                parsed = sdf.parse(dateStr)
                hasTime = p.contains("H")
                if (parsed != null) break
            } catch (_: ParseException) {
            }
        }
        if (parsed == null) return null
        val cal = Calendar.getInstance().apply { time = parsed }
        if (!hasTime) {
            // Asume 09:00 local si no viene hora
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        val begin = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val end = cal.timeInMillis
        return begin to end
    }
}
