package com.example.eventconnect.domain

import jakarta.persistence.*

@Entity
@Table(name = "user_reviews")
data class UserReview(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable=false) val reviewedUserEmail: String,
    @Column(nullable=false) val reviewerStore: String,
    @Column(nullable=false) val rating: Int,
    val comment: String? = null,
    @Column(nullable=false) val createdAt: Long = System.currentTimeMillis()
)

