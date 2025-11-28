package com.eventconnect.eventconnect.repository

import com.eventconnect.eventconnect.domain.enums.InterestType
import com.eventconnect.eventconnect.domain.model.UserInterest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserInterestRepository : JpaRepository<UserInterest, Long> {
    fun findByUserId(userId: Long): List<UserInterest>
    fun findByEventId(eventId: Long): List<UserInterest>
    fun findByUserIdAndEventId(userId: Long, eventId: Long): List<UserInterest>
    fun findByUserIdAndEventIdAndType(userId: Long, eventId: Long, type: InterestType): UserInterest?
    fun existsByUserIdAndEventIdAndType(userId: Long, eventId: Long, type: InterestType): Boolean
}

