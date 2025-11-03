package com.example.eventconnect.reminder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.eventconnect.notification.NotificationHelper

class EventReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Recordatorio de evento"
        val message = inputData.getString(KEY_MESSAGE) ?: "Tienes un evento hoy"
        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showEventReminder(applicationContext, title, message)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
    }
}

