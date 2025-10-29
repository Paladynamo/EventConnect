package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_ops")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,            // e.g., EVENT_CREATE, EVENT_UPDATE, EVENT_DELETE, RESERVATION_CREATE, RESERVATION_UPDATE, USER_UPDATE_NAME
    val payload: String,         // JSON con los datos mínimos para sincronizar
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

