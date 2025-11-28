package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.domain.enums.ReservationStatus
import com.eventconnect.eventconnect.domain.model.Event
import com.eventconnect.eventconnect.domain.model.Reservation
import com.eventconnect.eventconnect.dto.request.CreateReservationRequest
import com.eventconnect.eventconnect.dto.request.RejectReservationRequest
import com.eventconnect.eventconnect.dto.response.ReservationResponse
import com.eventconnect.eventconnect.exception.BadRequestException
import com.eventconnect.eventconnect.exception.ResourceNotFoundException
import com.eventconnect.eventconnect.exception.UnauthorizedException
import com.eventconnect.eventconnect.mapper.ReservationMapper
import com.eventconnect.eventconnect.repository.EventRepository
import com.eventconnect.eventconnect.repository.ReservationRepository
import com.eventconnect.eventconnect.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    fun createReservation(userId: Long, request: CreateReservationRequest): ReservationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado") }

        val event = eventRepository.findById(request.eventId)
            .orElseThrow { ResourceNotFoundException("Evento no encontrado") }

        // Verificar que no haya una reserva activa para este evento
        val existingReservations = reservationRepository.findByEventIdAndUserIdAndStatusIn(
            request.eventId,
            userId,
            listOf(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        )

        if (existingReservations.isNotEmpty()) {
            throw BadRequestException("Ya tienes una reserva activa para este evento")
        }

        // Verificar capacidad si aplica
        if (event.hasCapacity && event.capacityAvailable != null && event.capacityAvailable!! <= 0) {
            throw BadRequestException("El evento no tiene capacidad disponible")
        }

        val reservation = Reservation(
            eventId = request.eventId,
            userId = userId,
            userEmail = user.email,
            userName = user.name,
            status = ReservationStatus.PENDING
        )

        val savedReservation = reservationRepository.save(reservation)
        return ReservationMapper.toResponse(savedReservation)
    }

    fun getUserReservations(userId: Long): List<ReservationResponse> {
        val reservations = reservationRepository.findByUserId(userId)
        return reservations.map { ReservationMapper.toResponse(it) }
    }

    fun getEventReservations(eventId: Long, ownerId: Long): List<ReservationResponse> {
        val event = eventRepository.findById(eventId)
            .orElseThrow { ResourceNotFoundException("Evento no encontrado") }

        if (event.storeId != ownerId) {
            throw UnauthorizedException("No tienes permiso para ver las reservas de este evento")
        }

        val reservations = reservationRepository.findByEventId(eventId)
        return reservations.map { ReservationMapper.toResponse(it) }
    }

    fun approveReservation(reservationId: Long, ownerId: Long): ReservationResponse {
        val reservation = findReservationById(reservationId)
        val event = eventRepository.findById(reservation.eventId)
            .orElseThrow { ResourceNotFoundException("Evento no encontrado") }

        if (event.storeId != ownerId) {
            throw UnauthorizedException("No tienes permiso para aprobar esta reserva")
        }

        if (reservation.status != ReservationStatus.PENDING) {
            throw BadRequestException("Solo se pueden aprobar reservas pendientes")
        }

        reservation.status = ReservationStatus.APPROVED

        // Actualizar capacidad disponible si aplica
        if (event.hasCapacity && event.capacityAvailable != null) {
            event.capacityAvailable = event.capacityAvailable!! - 1
            eventRepository.save(event)
        }

        val updatedReservation = reservationRepository.save(reservation)
        return ReservationMapper.toResponse(updatedReservation)
    }

    fun rejectReservation(reservationId: Long, ownerId: Long, request: RejectReservationRequest): ReservationResponse {
        val reservation = findReservationById(reservationId)
        val event = eventRepository.findById(reservation.eventId)
            .orElseThrow { ResourceNotFoundException("Evento no encontrado") }

        if (event.storeId != ownerId) {
            throw UnauthorizedException("No tienes permiso para rechazar esta reserva")
        }

        if (reservation.status != ReservationStatus.PENDING) {
            throw BadRequestException("Solo se pueden rechazar reservas pendientes")
        }

        reservation.status = ReservationStatus.REJECTED
        reservation.reason = request.reason

        val updatedReservation = reservationRepository.save(reservation)
        return ReservationMapper.toResponse(updatedReservation)
    }

    private fun findReservationById(id: Long): Reservation {
        return reservationRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Reserva no encontrada con ID: $id") }
    }
}

