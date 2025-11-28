package com.eventconnect.eventconnect.repository

import com.eventconnect.eventconnect.domain.enums.EventCategory
import com.eventconnect.eventconnect.domain.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findByStoreId(storeId: Long): List<Event>
    fun findByRegionAndComuna(region: String, comuna: String): List<Event>
    fun findByCategory(category: EventCategory): List<Event>
    fun findByStoreIdAndCategory(storeId: Long, category: EventCategory): List<Event>
}

