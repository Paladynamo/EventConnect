package com.example.eventconnect.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventconnect.R
import com.example.eventconnect.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "eventconnect_reservations"
    private const val CHANNEL_NAME = "Reservas"
    private const val CHANNEL_DESC = "Notificaciones de reservas para organizadores"

    private const val REMINDER_CHANNEL_ID = "eventconnect_reminders"
    private const val REMINDER_CHANNEL_NAME = "Recordatorios"
    private const val REMINDER_CHANNEL_DESC = "Recordatorios de eventos para asistentes"

    private const val UPDATES_CHANNEL_ID = "eventconnect_updates"
    private const val UPDATES_CHANNEL_NAME = "Actualizaciones de eventos"
    private const val UPDATES_CHANNEL_DESC = "Avisos cuando un evento cambia información"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reservationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            manager.createNotificationChannel(reservationChannel)

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = REMINDER_CHANNEL_DESC }
            manager.createNotificationChannel(reminderChannel)

            val updatesChannel = NotificationChannel(
                UPDATES_CHANNEL_ID,
                UPDATES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = UPDATES_CHANNEL_DESC }
            manager.createNotificationChannel(updatesChannel)
        }
    }

    private fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun mainPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 1001, intent, flags)
    }

    private fun safeNotify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Silenciar si el usuario negó el permiso en Android 13+
        }
    }

    fun showReservationNotification(
        context: Context,
        organizerName: String?,
        eventTitle: String,
        reserverName: String
    ) {
        if (!canNotify(context)) return
        val title = "Nueva reserva" + (organizerName?.let { " para $it" } ?: "")
        val content = "$reserverName reservó un cupo en \"$eventTitle\""

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_eventconnect)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))

        safeNotify(context, (System.currentTimeMillis() % 10000).toInt(), notif)
    }

    fun showCancellationNotification(
        context: Context,
        organizerName: String?,
        eventTitle: String,
        reserverName: String
    ) {
        if (!canNotify(context)) return
        val title = "Cancelación de reserva" + (organizerName?.let { " en $it" } ?: "")
        val content = "$reserverName canceló su cupo en \"$eventTitle\""

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_eventconnect)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))

        safeNotify(context, (System.currentTimeMillis() % 10000).toInt(), notif)
    }

    fun showEventReminder(
        context: Context,
        eventTitle: String,
        message: String
    ) {
        if (!canNotify(context)) return
        val notif = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_eventconnect)
            .setContentTitle(eventTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))

        safeNotify(context, (System.currentTimeMillis() % 10000).toInt(), notif)
    }

    fun showEventUpdatedNotification(context: Context, eventTitle: String, store: String) {
        if (!canNotify(context)) return
        val title = "Evento actualizado"
        val content = "Se actualizaron datos en \"$eventTitle\" de $store. Toca para ver más."
        val notif = NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_eventconnect)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))

        safeNotify(context, (System.currentTimeMillis() % 10000).toInt(), notif)
    }

    fun showBulkEventUpdatesNotification(context: Context, count: Int) {
        if (!canNotify(context) || count <= 0) return
        val notif = NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_eventconnect)
            .setContentTitle("Actualizaciones de eventos")
            .setContentText("Hay $count eventos con cambios. Toca para ver.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context))

        safeNotify(context, (System.currentTimeMillis() % 10000).toInt(), notif)
    }
}
