package com.example.eventconnect.domain

import jakarta.persistence.*

@Entity
@Table(name = "reservations")
data class Reservation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable=false) val eventId: Long,
    @Column(nullable=false) val userEmail: String,
    @Column(nullable=false) val userName: String,
    @Column(nullable=false) val status: String = "PENDING", // PENDING APPROVED REJECTED CANCELLED
    val reason: String? = null,
    @Column(nullable=false) val createdAt: Long = System.currentTimeMillis()
)

