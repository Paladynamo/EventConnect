package com.example.eventconnect.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.EventUpdateNotice
import com.example.eventconnect.repository.EventRepository
import com.example.eventconnect.repository.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application): AndroidViewModel(app) {
    private val eventsRepo = EventRepository(app)
    private val noticesRepo = NoticeRepository(app)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _notices = MutableStateFlow<List<EventUpdateNotice>>(emptyList())
    val notices: StateFlow<List<EventUpdateNotice>> = _notices

    suspend fun loadAll() { _events.value = eventsRepo.getAll() }
    suspend fun loadByStore(store: String) { _events.value = eventsRepo.getByStore(store) }
    suspend fun loadByRegionComuna(region: String, comuna: String) { _events.value = eventsRepo.getByComuna(region, comuna) }
    suspend fun loadByRegionComunaCategory(region: String, comuna: String, category: String) { _events.value = eventsRepo.getByComunaAndCategory(region, comuna, category) }

    fun loadNotices(email: String) { viewModelScope.launch { _notices.value = noticesRepo.getForUser(email) } }
    fun clearNotices(ids: List<Int>) { viewModelScope.launch { noticesRepo.deleteByIds(ids) } }
}

