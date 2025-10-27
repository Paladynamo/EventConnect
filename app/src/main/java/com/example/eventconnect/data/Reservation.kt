package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val userEmail: String,
    val userName: String,
    val status: String, // PENDING, APPROVED, REJECTED
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

