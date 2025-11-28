package com.example.eventconnect.domain

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val name: String,
    @Column(nullable = false, unique = true) val email: String,
    @Column(nullable = false) val passwordHash: String,
    @Column(nullable = false) val userType: String, // USER or STORE
    val storeName: String? = null
)

