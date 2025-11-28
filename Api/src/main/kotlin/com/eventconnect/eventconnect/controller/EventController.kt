package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.domain.enums.EventCategory
import com.eventconnect.eventconnect.dto.request.CreateEventRequest
import com.eventconnect.eventconnect.dto.request.UpdateEventRequest
import com.eventconnect.eventconnect.dto.response.EventResponse
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.service.EventService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    @GetMapping
    fun getEvents(
        @RequestParam(required = false) region: String?,
        @RequestParam(required = false) comuna: String?,
        @RequestParam(required = false) category: EventCategory?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<EventResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val events = eventService.getAllEvents(region, comuna, category, pageable)
        return ResponseEntity.ok(events)
    }

    @GetMapping("/{id}")
    fun getEventById(@PathVariable id: Long): ResponseEntity<EventResponse> {
        val event = eventService.getEventById(id)
        return ResponseEntity.ok(event)
    }

    @PostMapping
    fun createEvent(
        @RequestParam userId: Long,
        @Valid @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> {
        val event = eventService.createEvent(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(event)
    }

    @PutMapping("/{id}")
    fun updateEvent(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @Valid @RequestBody request: UpdateEventRequest
    ): ResponseEntity<EventResponse> {
        val event = eventService.updateEvent(id, userId, request)
        return ResponseEntity.ok(event)
    }

    @DeleteMapping("/{id}")
    fun deleteEvent(
        @PathVariable id: Long,
        @RequestParam userId: Long
    ): ResponseEntity<MessageResponse> {
        eventService.deleteEvent(id, userId)
        return ResponseEntity.ok(MessageResponse("Evento eliminado exitosamente"))
    }

    @GetMapping("/store/{storeId}")
    fun getEventsByStore(@PathVariable storeId: Long): ResponseEntity<List<EventResponse>> {
        val events = eventService.getEventsByStore(storeId)
        return ResponseEntity.ok(events)
    }
}

