package com.eventconnect.eventconnect.repository

import com.eventconnect.eventconnect.domain.model.EventUpdateNotice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventUpdateNoticeRepository : JpaRepository<EventUpdateNotice, Long> {
    fun findByUserId(userId: Long): List<EventUpdateNotice>
    fun findByUserIdAndRead(userId: Long, read: Boolean): List<EventUpdateNotice>
    fun countByUserIdAndRead(userId: Long, read: Boolean): Long
    fun findByEventId(eventId: Long): List<EventUpdateNotice>
}

