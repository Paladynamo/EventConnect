package com.example.eventconnect.repository

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.PendingOperation

class PendingRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).pendingOperationDao()

    suspend fun enqueue(type: String, payload: String) {
        dao.insert(PendingOperation(type = type, payload = payload))
    }
}
