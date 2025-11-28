package com.example.eventconnect.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.EventUpdateNotice
import com.example.eventconnect.repository.EventRepository
import com.example.eventconnect.repository.InterestRepository
import com.example.eventconnect.repository.NoticeRepository
import com.example.eventconnect.repository.PendingRepository
import kotlinx.coroutines.launch

class EditEventViewModel(app: Application): AndroidViewModel(app) {
    private val events = EventRepository(app)
    private val interests = InterestRepository(app)
    private val notices = NoticeRepository(app)
    private val pending = PendingRepository(app)

    suspend fun getEvent(id: Int) = events.getById(id)
    suspend fun update(event: Event) = events.update(event)
    suspend fun delete(event: Event) = events.delete(event)

    fun queueUpdatePayload(eventId: Int, title: String, category: String) {
        viewModelScope.launch { pending.enqueue("EVENT_UPDATE", "{\"id\":${eventId},\"title\":\"$title\",\"category\":\"$category\"}") }
    }
    fun queueDelete(eventId: Int) {
        viewModelScope.launch { pending.enqueue("EVENT_DELETE", "{\"id\":${eventId}}") }
    }

    suspend fun notifyInterested(eventId: Int, title: String, store: String, changes: String) {
        val ints = interests.getByEvent(eventId)
        ints.forEach { i -> notices.insert(EventUpdateNotice(userEmail = i.userEmail, eventId = eventId, eventTitle = title, store = store, changes = changes)) }
    }
}

