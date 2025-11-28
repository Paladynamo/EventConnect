package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.domain.enums.EventCategory
import com.eventconnect.eventconnect.domain.model.Event
import com.eventconnect.eventconnect.dto.request.CreateEventRequest
import com.eventconnect.eventconnect.dto.request.UpdateEventRequest
import com.eventconnect.eventconnect.dto.response.EventResponse
import com.eventconnect.eventconnect.exception.BadRequestException
import com.eventconnect.eventconnect.exception.ResourceNotFoundException
import com.eventconnect.eventconnect.exception.UnauthorizedException
import com.eventconnect.eventconnect.mapper.EventMapper
import com.eventconnect.eventconnect.repository.EventRepository
import com.eventconnect.eventconnect.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    fun getAllEvents(region: String?, comuna: String?, category: EventCategory?, pageable: Pageable): Page<EventResponse> {
        val events = when {
            region != null && comuna != null -> eventRepository.findByRegionAndComuna(region, comuna)
            category != null -> eventRepository.findByCategory(category)
            else -> eventRepository.findAll()
        }
        
        // Filtrar por categoría si se especifica
        val filteredEvents = if (category != null && (region != null || comuna != null)) {
            events.filter { it.category == category }
        } else {
            events
        }
        
        // Aplicar paginación manual
        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, filteredEvents.size)
        val paginatedEvents = if (start < filteredEvents.size) {
            filteredEvents.subList(start, end)
        } else {
            emptyList()
        }
        
        return PageImpl(
            paginatedEvents.map { EventMapper.toResponse(it) },
            pageable,
            filteredEvents.size.toLong()
        )
    }

    fun getEventById(id: Long): EventResponse {
        val event = findEventById(id)
        return EventMapper.toResponse(event)
    }

    fun createEvent(userId: Long, request: CreateEventRequest): EventResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado") }

        if (user.userType != com.eventconnect.eventconnect.domain.enums.UserType.TIENDA) {
            throw UnauthorizedException("Solo las tiendas pueden crear eventos")
        }

        val event = EventMapper.fromCreateRequest(request, userId, user.storeName ?: user.name)
        val savedEvent = eventRepository.save(event)
        return EventMapper.toResponse(savedEvent)
    }

    fun updateEvent(eventId: Long, userId: Long, request: UpdateEventRequest): EventResponse {
        val event = findEventById(eventId)

        if (event.storeId != userId) {
            throw UnauthorizedException("No tienes permiso para modificar este evento")
        }

        EventMapper.updateFromRequest(event, request)
        val updatedEvent = eventRepository.save(event)
        return EventMapper.toResponse(updatedEvent)
    }

    fun deleteEvent(eventId: Long, userId: Long) {
        val event = findEventById(eventId)

        if (event.storeId != userId) {
            throw UnauthorizedException("No tienes permiso para eliminar este evento")
        }

        eventRepository.delete(event)
    }

    fun getEventsByStore(storeId: Long): List<EventResponse> {
        val events = eventRepository.findByStoreId(storeId)
        return events.map { EventMapper.toResponse(it) }
    }

    private fun findEventById(id: Long): Event {
        return eventRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Evento no encontrado con ID: $id") }
    }
}

