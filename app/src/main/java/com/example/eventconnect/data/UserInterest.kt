package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_interests",
    indices = [Index(value = ["userEmail", "eventId", "type"], unique = true)]
)
data class UserInterest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val eventId: Int,
    val type: String // "LIKE" o "APPROVED"
)

