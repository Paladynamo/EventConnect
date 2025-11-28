package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reviews")
data class UserReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reviewedUserEmail: String,
    val reviewerStore: String,
    val rating: Int, // 1..5
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

