package com.example.eventconnect.repository

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Reservation

class ReservationRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).reservationDao()

    suspend fun getForUser(email: String) = dao.getForUser(email)
    suspend fun getLatest(eventId: Int, email: String) = dao.getLatestForEventAndUser(eventId, email)
    suspend fun getById(id: Int) = dao.getById(id)
    suspend fun update(res: Reservation) = dao.update(res)
    suspend fun insert(res: Reservation) = dao.insert(res)
    suspend fun getForEvents(eventIds: List<Int>) = dao.getForEvents(eventIds)
}
