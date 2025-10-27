package com.example.eventconnect.data

import androidx.room.*

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: Event)

    @Query("SELECT * FROM events ORDER BY id DESC")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE store = :store ORDER BY id DESC")
    suspend fun getEventsByStore(store: String): List<Event>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Int): Event?

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)
}
