package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.dto.request.CreateReservationRequest
import com.eventconnect.eventconnect.dto.request.RejectReservationRequest
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.dto.response.ReservationResponse
import com.eventconnect.eventconnect.service.ReservationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {

    @PostMapping
    fun createReservation(
        @RequestParam userId: Long,
        @Valid @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.createReservation(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation)
    }

    @GetMapping("/user")
    fun getUserReservations(@RequestParam userId: Long): ResponseEntity<List<ReservationResponse>> {
        val reservations = reservationService.getUserReservations(userId)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/event/{eventId}")
    fun getEventReservations(
        @PathVariable eventId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<List<ReservationResponse>> {
        val reservations = reservationService.getEventReservations(eventId, userId)
        return ResponseEntity.ok(reservations)
    }

    @PutMapping("/{id}/approve")
    fun approveReservation(
        @PathVariable id: Long,
        @RequestParam userId: Long
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.approveReservation(id, userId)
        return ResponseEntity.ok(reservation)
    }

    @PutMapping("/{id}/reject")
    fun rejectReservation(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @Valid @RequestBody request: RejectReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.rejectReservation(id, userId, request)
        return ResponseEntity.ok(reservation)
    }
}

