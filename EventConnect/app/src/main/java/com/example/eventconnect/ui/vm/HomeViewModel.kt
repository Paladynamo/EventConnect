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
import com.example.eventconnect.network.RemoteEventsApi
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.dto.toEvents
import kotlinx.coroutines.launch

class HomeViewModel(app: Application, private val eventServiceProvider: (() -> EventService)? = null): AndroidViewModel(app) {
    private val eventsRepo = EventRepository(app)
    private val noticesRepo = NoticeRepository(app)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _notices = MutableStateFlow<List<EventUpdateNotice>>(emptyList())
    val notices: StateFlow<List<EventUpdateNotice>> = _notices

    private val eventService: EventService by lazy { eventServiceProvider?.invoke() ?: RetrofitProvider.retrofit(getApplication()).create(EventService::class.java) }

    private fun sessionToken(): String? {
        val prefs = getApplication<Application>().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        return prefs.getString("token", null)
    }

    suspend fun loadAll() { 
        _events.value = try { 
            val pageResponse = eventService.list()
            pageResponse.content.toEvents()
        } catch (_: Exception) { 
            eventsRepo.getAll() 
        } 
    }
    
    suspend fun loadByStore(storeId: Long) { 
        _events.value = try { 
            eventService.getByStore(storeId).toEvents()
        } catch (_: Exception) { 
            eventsRepo.getByStore(storeId.toString()) 
        } 
    }
    
    suspend fun loadByRegion(region: String) { 
        _events.value = try { 
            val pageResponse = eventService.list(region = region)
            pageResponse.content.toEvents()
        } catch (_: Exception) { 
            eventsRepo.getByRegion(region) 
        } 
    }
    
    suspend fun loadByRegionCategory(region: String, category: String) { 
        _events.value = try { 
            val pageResponse = eventService.list(region = region, category = category)
            pageResponse.content.toEvents()
        } catch (_: Exception) { 
            eventsRepo.getByRegionAndCategory(region, category) 
        } 
    }
    
    suspend fun loadByRegionComuna(region: String, comuna: String) { 
        _events.value = try { 
            val pageResponse = eventService.list(region = region, comuna = comuna)
            pageResponse.content.toEvents()
        } catch (_: Exception) { 
            eventsRepo.getByComuna(region, comuna) 
        } 
    }
    
    suspend fun loadByRegionComunaCategory(region: String, comuna: String, category: String) { 
        _events.value = try { 
            val pageResponse = eventService.list(region = region, comuna = comuna, category = category)
            pageResponse.content.toEvents()
        } catch (_: Exception) { 
            eventsRepo.getByComunaAndCategory(region, comuna, category) 
        } 
    }

    fun loadNotices(email: String) { viewModelScope.launch { _notices.value = noticesRepo.getForUser(email) } }
    fun clearNotices(ids: List<Int>) { viewModelScope.launch { noticesRepo.deleteByIds(ids) } }
}
