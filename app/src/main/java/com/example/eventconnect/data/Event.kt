package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // ✅ Asegurado como String (no Char)
    val date: String,
    val store: String,
    val imageUri: String? = null,
    val hasCapacity: Boolean = false,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null
)
