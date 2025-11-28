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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventDetailViewModel(app: Application): AndroidViewModel(app) {
    private val events = EventRepository(app)
    private val reservations = ReservationRepository(app)
    private val interests = InterestRepository(app)
    private val pending = PendingRepository(app)

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    private val _myReservation = MutableStateFlow<Reservation?>(null)
    val myReservation: StateFlow<Reservation?> = _myReservation

    fun load(eventId: Int, userEmail: String) {
        viewModelScope.launch {
            _event.value = events.getById(eventId)
            _myReservation.value = reservations.getLatest(eventId, userEmail)
        }
    }

    fun requestSeat(event: Event, userEmail: String, userName: String, onDone: ()->Unit) {
        viewModelScope.launch {
            reservations.insert(Reservation(eventId = event.id, userEmail = userEmail, userName = userName, status = "PENDING"))
            pending.enqueue("RESERVATION_CREATE", "{\"eventId\":${event.id},\"userEmail\":\"$userEmail\"}")
            onDone()
        }
    }

    fun likeEvent(eventId: Int, userEmail: String) {
        viewModelScope.launch { interests.insert(UserInterest(userEmail = userEmail, eventId = eventId, type = "LIKE")) }
    }
}

