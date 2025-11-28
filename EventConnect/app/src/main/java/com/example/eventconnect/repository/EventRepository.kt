package com.example.eventconnect.repository

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.Event

class EventRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).eventDao()

    suspend fun getAll() = dao.getAllEvents()
    suspend fun getByStore(store: String) = dao.getEventsByStore(store)
    suspend fun getByComuna(region: String, comuna: String) = dao.getEventsByComuna(region, comuna)
    suspend fun getByComunaAndCategory(region: String, comuna: String, category: String) = dao.getEventsByComunaAndCategory(region, comuna, category)
    suspend fun getByRegion(region: String) = dao.getEventsByRegion(region)
    suspend fun getByRegionAndCategory(region: String, category: String) = dao.getEventsByRegionAndCategory(region, category)
    suspend fun getById(id: Int) = dao.getEventById(id)
    suspend fun insert(event: Event) = dao.insertEvent(event)
    suspend fun update(event: Event) = dao.updateEvent(event)
    suspend fun delete(event: Event) = dao.deleteEvent(event)
}
