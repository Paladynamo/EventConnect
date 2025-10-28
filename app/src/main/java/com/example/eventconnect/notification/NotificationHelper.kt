package com.example.eventconnect.notification

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventconnect.R

object NotificationHelper {
    private const val CHANNEL_ID = "eventconnect_reservations"
    private const val CHANNEL_NAME = "Reservas"
    private const val CHANNEL_DESC = "Notificaciones de reservas para organizadores"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
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
            .build()

        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 10000).toInt(), notif)
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
            .build()

        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 10000).toInt(), notif)
    }
}
