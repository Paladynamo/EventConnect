package com.example.eventconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: UserReview): Long

    @Query("SELECT * FROM user_reviews WHERE reviewedUserEmail = :email ORDER BY createdAt DESC")
    suspend fun getReviewsForUser(email: String): List<UserReview>

    @Query("SELECT AVG(rating) FROM user_reviews WHERE reviewedUserEmail = :email")
    suspend fun getAverageRating(email: String): Double?

    @Query("SELECT COUNT(*) FROM user_reviews WHERE reviewedUserEmail = :email")
    suspend fun getReviewsCount(email: String): Int
}

