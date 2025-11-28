package com.example.eventconnect

import android.app.Application
import com.example.eventconnect.notification.NotificationHelper
import com.example.eventconnect.sync.SyncManager

class EventConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        // No-op: sincronización futura
        SyncManager.runOnce()
    }
}
