package com.example.eventconnect.repository

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.EventUpdateNotice

class NoticeRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).eventUpdateNoticeDao()

    suspend fun insert(n: EventUpdateNotice) = dao.insert(n)
    suspend fun getForUser(email: String): List<EventUpdateNotice> = dao.getForUser(email)
    suspend fun deleteByIds(ids: List<Int>) = dao.deleteByIds(ids)
}
