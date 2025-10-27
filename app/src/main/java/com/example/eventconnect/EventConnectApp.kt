package com.example.eventconnect

import android.app.Application
import com.example.eventconnect.notification.NotificationHelper

class EventConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
