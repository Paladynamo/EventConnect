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
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.dto.UpdateEventRequest
import com.example.eventconnect.remote.dto.toEvent
import com.example.eventconnect.util.SessionHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class EditEventViewModel(app: Application): AndroidViewModel(app) {
    private val events = EventRepository(app)
    private val interests = InterestRepository(app)
    private val notices = NoticeRepository(app)
    private val pending = PendingRepository(app)
    
    private val eventService: EventService by lazy {
        RetrofitProvider.retrofit(getApplication()).create(EventService::class.java)
    }

    suspend fun getEvent(id: Int) = events.getById(id)
    
    suspend fun update(event: Event): Result<Event> {
        val userId = SessionHelper.getUserId(getApplication())
        if (userId == null) {
            // Fallback: actualizar solo localmente
            events.update(event)
            return Result.failure(Exception("No se encontró sesión de usuario"))
        }
        
        return try {
            // Convertir fecha a ISO 8601 si es necesario
            val dateMillis = parseDateToMillis(event.date)
            val isoDate = if (dateMillis != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                String.format(
                    Locale.US,
                    "%04d-%02d-%02dT%02d:%02d:%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    9, 0, 0
                )
            } else {
                event.date // Si ya está en formato ISO, usar directamente
            }
            
            Log.d("EditEventViewModel", "Actualizando evento ${event.id} en API")
            
            val updateRequest = UpdateEventRequest(
                title = event.title,
                description = event.description,
                category = event.category,
                date = isoDate,
                region = event.region,
                comuna = event.comuna,
                addressStreet = event.addressStreet,
                addressNumber = event.addressNumber,
                imageUrls = event.imageUris,
                hasCapacity = event.hasCapacity,
                capacityTotal = event.capacityTotal,
                capacityAvailable = event.capacityAvailable
            )
            
            val response = withContext(Dispatchers.IO) {
                eventService.update(event.id.toLong(), userId, updateRequest)
            }
            
            Log.d("EditEventViewModel", "Evento actualizado en API: ${response.id}")
            
            // Guardar el evento actualizado en la base de datos local
            val updatedEvent = response.toEvent()
            events.update(updatedEvent)
            
            Result.success(updatedEvent)
        } catch (e: HttpException) {
            Log.e("EditEventViewModel", "Error HTTP ${e.code()}: ${e.message()}", e)
            // Fallback: actualizar localmente
            events.update(event)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("EditEventViewModel", "Error al actualizar evento: ${e.message}", e)
            // Fallback: actualizar localmente
            events.update(event)
            Result.failure(e)
        }
    }
    
    suspend fun delete(event: Event): Result<Unit> {
        val userId = SessionHelper.getUserId(getApplication())
        if (userId == null) {
            // Fallback: eliminar solo localmente
            events.delete(event)
            return Result.failure(Exception("No se encontró sesión de usuario"))
        }
        
        return try {
            Log.d("EditEventViewModel", "Eliminando evento ${event.id} en API")
            
            withContext(Dispatchers.IO) {
                eventService.delete(event.id.toLong(), userId)
            }
            
            Log.d("EditEventViewModel", "Evento eliminado en API: ${event.id}")
            
            // Eliminar de la base de datos local
            events.delete(event)
            
            Result.success(Unit)
        } catch (e: HttpException) {
            Log.e("EditEventViewModel", "Error HTTP ${e.code()}: ${e.message()}", e)
            // Fallback: eliminar localmente
            events.delete(event)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("EditEventViewModel", "Error al eliminar evento: ${e.message}", e)
            // Fallback: eliminar localmente
            events.delete(event)
            Result.failure(e)
        }
    }
    
    private fun parseDateToMillis(dateStr: String): Long? {
        val patterns = listOf("d/M/yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
        for (p in patterns) {
            val sdf = SimpleDateFormat(p, Locale.getDefault())
            sdf.isLenient = false
            try {
                val d = sdf.parse(dateStr) ?: continue
                return d.time
            } catch (_: Exception) { }
        }
        return null
    }

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

