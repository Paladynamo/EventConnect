package com.eventconnect.eventconnect.repository

import com.eventconnect.eventconnect.domain.enums.ReservationStatus
import com.eventconnect.eventconnect.domain.model.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByEventId(eventId: Long): List<Reservation>
    fun findByUserId(userId: Long): List<Reservation>
    fun findByEventIdAndUserId(eventId: Long, userId: Long): List<Reservation>
    fun findByEventIdAndUserIdAndStatusIn(
        eventId: Long,
        userId: Long,
        statuses: List<ReservationStatus>
    ): List<Reservation>
    fun countByEventIdAndStatus(eventId: Long, status: ReservationStatus): Long
}

