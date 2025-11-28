package com.eventconnect.eventconnect.mapper

import com.eventconnect.eventconnect.domain.model.EventUpdateNotice
import com.eventconnect.eventconnect.dto.response.NotificationResponse

object NotificationMapper {
    fun toResponse(notice: EventUpdateNotice): NotificationResponse {
        return NotificationResponse(
            id = notice.id!!,
            eventId = notice.eventId,
            eventTitle = notice.eventTitle,
            store = notice.store,
            changes = notice.changes,
            read = notice.read,
            createdAt = notice.createdAt
        )
    }
}

