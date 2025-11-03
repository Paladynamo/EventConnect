package com.example.eventconnect.repository

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.UserInterest

class InterestRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).userInterestDao()

    suspend fun insert(interest: UserInterest) = dao.insert(interest)
    suspend fun getByEvent(eventId: Int) = dao.getByEvent(eventId)
    suspend fun count(email: String, eventId: Int, type: String) = dao.count(email, eventId, type)
}
