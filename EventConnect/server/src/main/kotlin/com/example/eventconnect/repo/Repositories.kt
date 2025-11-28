package com.example.eventconnect.repo

import com.example.eventconnect.domain.*
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepo: JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}

interface EventRepo: JpaRepository<Event, Long> {
    fun findByStoreName(storeName: String): List<Event>
    fun findByRegion(region: String): List<Event>
    fun findByRegionAndCategory(region: String, category: String): List<Event>
    fun findByRegionAndComuna(region: String, comuna: String): List<Event>
    fun findByRegionAndComunaAndCategory(region: String, comuna: String, category: String): List<Event>
}

interface ReservationRepo: JpaRepository<Reservation, Long> {
    fun findByEventId(eventId: Long): List<Reservation>
    fun findByUserEmail(userEmail: String): List<Reservation>
}

interface UserReviewRepo: JpaRepository<UserReview, Long> {
    fun findByReviewedUserEmail(reviewedUserEmail: String): List<UserReview>
}

