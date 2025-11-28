package com.example.eventconnect.data

import androidx.room.*

@Dao
interface ReservationDao {
    @Insert
    suspend fun insert(reservation: Reservation)

    @Query("SELECT * FROM reservations WHERE eventId IN (:eventIds) ORDER BY createdAt DESC")
    suspend fun getForEvents(eventIds: List<Int>): List<Reservation>

    @Query("SELECT * FROM reservations WHERE userEmail = :email ORDER BY createdAt DESC")
    suspend fun getForUser(email: String): List<Reservation>

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Reservation?

    @Update
    suspend fun update(reservation: Reservation)

    // Nuevas utilidades
    @Query("SELECT * FROM reservations WHERE eventId = :eventId AND userEmail = :email ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForEventAndUser(eventId: Int, email: String): Reservation?

    @Query("SELECT COUNT(*) FROM reservations WHERE eventId = :eventId AND userEmail = :email AND status = :status")
    suspend fun countByStatusForEventAndUser(eventId: Int, email: String, status: String): Int
}
