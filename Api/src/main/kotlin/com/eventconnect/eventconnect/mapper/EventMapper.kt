package com.eventconnect.eventconnect.mapper

import com.eventconnect.eventconnect.domain.model.Event
import com.eventconnect.eventconnect.dto.request.CreateEventRequest
import com.eventconnect.eventconnect.dto.request.UpdateEventRequest
import com.eventconnect.eventconnect.dto.response.EventResponse

object EventMapper {
    fun toResponse(event: Event): EventResponse {
        return EventResponse(
            id = event.id!!,
            title = event.title,
            description = event.description,
            category = event.category,
            date = event.date,
            storeId = event.storeId,
            storeName = event.storeName,
            region = event.region,
            comuna = event.comuna,
            addressStreet = event.addressStreet,
            addressNumber = event.addressNumber,
            imageUrls = event.imageUrls,
            hasCapacity = event.hasCapacity,
            capacityTotal = event.capacityTotal,
            capacityAvailable = event.capacityAvailable,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt
        )
    }

    fun fromCreateRequest(request: CreateEventRequest, storeId: Long, storeName: String): Event {
        return Event(
            title = request.title,
            description = request.description,
            category = request.category,
            date = request.date,
            storeId = storeId,
            storeName = storeName,
            region = request.region,
            comuna = request.comuna,
            addressStreet = request.addressStreet,
            addressNumber = request.addressNumber,
            imageUrls = request.imageUrls,
            hasCapacity = request.hasCapacity,
            capacityTotal = request.capacityTotal,
            capacityAvailable = request.capacityTotal
        )
    }

    fun updateFromRequest(event: Event, request: UpdateEventRequest) {
        request.title?.let { event.title = it }
        request.description?.let { event.description = it }
        request.category?.let { event.category = it }
        request.date?.let { event.date = it }
        request.region?.let { event.region = it }
        request.comuna?.let { event.comuna = it }
        request.addressStreet?.let { event.addressStreet = it }
        request.addressNumber?.let { event.addressNumber = it }
        request.imageUrls?.let { event.imageUrls = it }
        request.hasCapacity?.let { event.hasCapacity = it }
        request.capacityTotal?.let { event.capacityTotal = it }
        request.capacityAvailable?.let { event.capacityAvailable = it }
    }
}

