package com.example.eventconnect.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.data.UserInterest
import com.example.eventconnect.repository.EventRepository
import com.example.eventconnect.repository.InterestRepository
import com.example.eventconnect.repository.PendingRepository
import com.example.eventconnect.repository.ReservationRepository
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.service.ReservationService
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.dto.CreateReservationRequest
import com.example.eventconnect.remote.dto.toReservation
import com.example.eventconnect.remote.dto.toEvent
import com.example.eventconnect.util.SessionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import android.util.Log

class EventDetailViewModel(app: Application): AndroidViewModel(app) {
    private val events = EventRepository(app)
    private val reservations = ReservationRepository(app)
    private val interests = InterestRepository(app)
    private val pending = PendingRepository(app)
    
    private val reservationService: ReservationService by lazy {
        RetrofitProvider.retrofit(getApplication()).create(ReservationService::class.java)
    }
    
    private val eventService: EventService by lazy {
        RetrofitProvider.retrofit(getApplication()).create(EventService::class.java)
    }

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    private val _myReservation = MutableStateFlow<Reservation?>(null)
    val myReservation: StateFlow<Reservation?> = _myReservation

    fun load(eventId: Int, userEmail: String) {
        viewModelScope.launch {
            // Primero intentar desde la base de datos local
            var event = events.getById(eventId)
            
            // Si no está en local, intentar desde la API
            if (event == null) {
                try {
                    Log.d("EventDetailViewModel", "Evento no encontrado localmente, buscando en API con id=$eventId")
                    val response = withContext(Dispatchers.IO) {
                        eventService.getById(eventId.toLong())
                    }
                    event = response.toEvent()
                    
                    // Guardar en la base de datos local para futuras consultas
                    events.insert(event)
                    
                    Log.d("EventDetailViewModel", "Evento cargado desde API: ${event.title}")
                } catch (e: Exception) {
                    Log.e("EventDetailViewModel", "Error al cargar evento desde API: ${e.message}", e)
                }
            }
            
            _event.value = event
            
            // Cargar reserva del usuario para este evento
            // Primero intentar desde local
            var reservation = reservations.getLatest(eventId, userEmail)
            
            // Si no está en local, intentar desde la API
            val userId = SessionHelper.getUserId(getApplication())
            if (reservation == null && userId != null) {
                try {
                    val userReservations = withContext(Dispatchers.IO) {
                        reservationService.getUserReservations(userId)
                    }
                    // Buscar la reserva para este evento
                    val eventReservation = userReservations.firstOrNull { it.eventId.toInt() == eventId }
                    if (eventReservation != null) {
                        reservation = eventReservation.toReservation()
                        // Guardar en local
                        reservations.insert(reservation!!)
                        Log.d("EventDetailViewModel", "Reserva cargada desde API para evento $eventId")
                    }
                } catch (e: Exception) {
                    Log.e("EventDetailViewModel", "Error al cargar reserva desde API: ${e.message}", e)
                }
            }
            
            _myReservation.value = reservation
        }
    }

    fun requestSeat(event: Event, userEmail: String, userName: String, onDone: ()->Unit) {
        viewModelScope.launch {
            val userId = SessionHelper.getUserId(getApplication())
            if (userId == null) {
                // Si no hay userId, guardar localmente como fallback
                reservations.insert(Reservation(eventId = event.id, userEmail = userEmail, userName = userName, status = "PENDING"))
                pending.enqueue("RESERVATION_CREATE", "{\"eventId\":${event.id},\"userEmail\":\"$userEmail\"}")
                onDone()
                return@launch
            }
            
            try {
                Log.d("EventDetailViewModel", "Creando reserva en API para evento ${event.id}, userId=$userId")
                
                val createRequest = CreateReservationRequest(eventId = event.id.toLong())
                val response = withContext(Dispatchers.IO) {
                    reservationService.create(userId, createRequest)
                }
                
                Log.d("EventDetailViewModel", "Reserva creada en API: id=${response.id}, status=${response.status}")
                
                // Guardar la reserva retornada por la API en la base de datos local
                withContext(Dispatchers.IO) {
                    val reservation = response.toReservation()
                    reservations.insert(reservation)
                }
                
                // Actualizar el estado local
                _myReservation.value = response.toReservation()
                
                onDone()
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    400 -> "Error: Datos inválidos. Verifica que el evento tenga cupos disponibles."
                    401 -> "Error: No autorizado. Por favor, inicia sesión nuevamente."
                    403 -> "Error: No tienes permiso para crear esta reserva."
                    404 -> "Error: Evento no encontrado."
                    409 -> "Error: Ya tienes una reserva para este evento."
                    500 -> "Error del servidor. Por favor, intenta más tarde."
                    else -> "Error HTTP ${e.code()}: ${e.message()}"
                }
                Log.e("EventDetailViewModel", "Error HTTP ${e.code()}: ${e.message()}", e)
                // Guardar localmente como fallback
                reservations.insert(Reservation(eventId = event.id, userEmail = userEmail, userName = userName, status = "PENDING"))
                pending.enqueue("RESERVATION_CREATE", "{\"eventId\":${event.id},\"userEmail\":\"$userEmail\"}")
                onDone()
            } catch (e: java.net.UnknownHostException) {
                Log.e("EventDetailViewModel", "Error de conexión: ${e.message}", e)
                // Guardar localmente como fallback
                reservations.insert(Reservation(eventId = event.id, userEmail = userEmail, userName = userName, status = "PENDING"))
                pending.enqueue("RESERVATION_CREATE", "{\"eventId\":${event.id},\"userEmail\":\"$userEmail\"}")
                onDone()
            } catch (e: Exception) {
                Log.e("EventDetailViewModel", "Error inesperado al crear reserva: ${e.message}", e)
                // Guardar localmente como fallback
                reservations.insert(Reservation(eventId = event.id, userEmail = userEmail, userName = userName, status = "PENDING"))
                pending.enqueue("RESERVATION_CREATE", "{\"eventId\":${event.id},\"userEmail\":\"$userEmail\"}")
                onDone()
            }
        }
    }

    fun likeEvent(eventId: Int, userEmail: String) {
        viewModelScope.launch { interests.insert(UserInterest(userEmail = userEmail, eventId = eventId, type = "LIKE")) }
    }

    fun deleteEvent(event: Event, onDone: () -> Unit) {
        viewModelScope.launch {
            val userId = SessionHelper.getUserId(getApplication())
            if (userId == null) {
                // Fallback: eliminar solo localmente
                events.delete(event)
                pending.enqueue("EVENT_DELETE", "{\"id\":${event.id}}")
                onDone()
                return@launch
            }
            
            try {
                Log.d("EventDetailViewModel", "Eliminando evento ${event.id} en API")
                
                withContext(Dispatchers.IO) {
                    eventService.delete(event.id.toLong(), userId)
                }
                
                Log.d("EventDetailViewModel", "Evento eliminado en API: ${event.id}")
                
                // Eliminar de la base de datos local
                events.delete(event)
                
                onDone()
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    400 -> "Error: Datos inválidos."
                    401 -> "Error: No autorizado. Por favor, inicia sesión nuevamente."
                    403 -> "Error: No tienes permiso para eliminar este evento."
                    404 -> "Error: Evento no encontrado."
                    500 -> "Error del servidor. Por favor, intenta más tarde."
                    else -> "Error HTTP ${e.code()}: ${e.message()}"
                }
                Log.e("EventDetailViewModel", "Error HTTP ${e.code()}: ${e.message()}", e)
                // Fallback: eliminar localmente
                events.delete(event)
                pending.enqueue("EVENT_DELETE", "{\"id\":${event.id}}")
                onDone()
            } catch (e: Exception) {
                Log.e("EventDetailViewModel", "Error al eliminar evento: ${e.message}", e)
                // Fallback: eliminar localmente
                events.delete(event)
                pending.enqueue("EVENT_DELETE", "{\"id\":${event.id}}")
                onDone()
            }
        }
    }
}
