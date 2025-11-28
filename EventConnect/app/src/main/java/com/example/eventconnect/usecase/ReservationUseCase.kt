package com.example.eventconnect.usecase

import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation

class ReservationUseCase {
    fun approve(reservation: Reservation, event: Event): Pair<Reservation, Event> {
        if (!event.hasCapacity) return reservation.copy(status="APPROVED") to event
        val avail = (event.capacityAvailable ?: 0)
        require(avail > 0) { "Sin cupos" }
        return reservation.copy(status="APPROVED") to event.copy(capacityAvailable = avail - 1)
    }
    fun cancel(reservation: Reservation, event: Event): Pair<Reservation, Event> {
        return if (reservation.status == "APPROVED" && event.hasCapacity) {
            reservation.copy(status="CANCELLED") to event.copy(capacityAvailable = (event.capacityAvailable ?: 0) + 1)
        } else reservation.copy(status="CANCELLED") to event
    }
    fun reject(reservation: Reservation, reason: String): Reservation = reservation.copy(status="REJECTED", reason = reason)
}
