package com.example.eventconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EventUpdateNoticeDao {
    @Insert
    suspend fun insert(notice: EventUpdateNotice)

    @Query("SELECT * FROM event_update_notices WHERE userEmail = :email ORDER BY createdAt DESC")
    suspend fun getForUser(email: String): List<EventUpdateNotice>

    @Query("DELETE FROM event_update_notices WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    // Métodos síncronos para usos puntuales
    @Query("SELECT * FROM event_update_notices WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getForUserSync(email: String): List<EventUpdateNotice>

    @Query("DELETE FROM event_update_notices WHERE id IN (:ids)")
    fun deleteByIdsSync(ids: List<Int>)
}
