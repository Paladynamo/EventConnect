package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_update_notices")
data class EventUpdateNotice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val eventId: Int,
    val eventTitle: String,
    val store: String,
    val changes: String,
    val createdAt: Long = System.currentTimeMillis()
)

