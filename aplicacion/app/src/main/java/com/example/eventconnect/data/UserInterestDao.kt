package com.example.eventconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserInterestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(interest: UserInterest)

    @Query("SELECT * FROM user_interests WHERE eventId = :eventId")
    suspend fun getByEvent(eventId: Int): List<UserInterest>

    @Query("SELECT COUNT(*) FROM user_interests WHERE userEmail = :email AND eventId = :eventId AND type = :type")
    suspend fun count(email: String, eventId: Int, type: String): Int
}

