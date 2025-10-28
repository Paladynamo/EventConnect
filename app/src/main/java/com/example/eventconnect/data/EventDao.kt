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

    // Nuevos filtros por ubicación y categoría
    @Query("SELECT * FROM events WHERE region = :region AND comuna = :comuna ORDER BY id DESC")
    suspend fun getEventsByComuna(region: String, comuna: String): List<Event>

    @Query("SELECT * FROM events WHERE region = :region AND comuna = :comuna AND category = :category ORDER BY id DESC")
    suspend fun getEventsByComunaAndCategory(region: String, comuna: String, category: String): List<Event>

    @Query("SELECT DISTINCT category FROM events ORDER BY category ASC")
    suspend fun getAvailableCategories(): List<String>
}
