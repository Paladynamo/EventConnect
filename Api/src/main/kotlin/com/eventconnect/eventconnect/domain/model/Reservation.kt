package com.eventconnect.eventconnect.domain.model

import com.eventconnect.eventconnect.domain.enums.ReservationStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "reservations",
    indexes = [
        Index(name = "idx_event_id", columnList = "event_id"),
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_event_user", columnList = "event_id,user_id")
    ]
)
data class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "user_email", nullable = false)
    var userEmail: String = "",

    @Column(name = "user_name", nullable = false)
    var userName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus = ReservationStatus.PENDING,

    @Column(nullable = true, columnDefinition = "TEXT")
    var reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = null,
        eventId = 0,
        userId = 0,
        userEmail = "",
        userName = "",
        status = ReservationStatus.PENDING,
        reason = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

