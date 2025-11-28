package com.example.eventconnect.web

import com.example.eventconnect.domain.Reservation
import com.example.eventconnect.repo.ReservationRepo
import com.example.eventconnect.repo.EventRepo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reservations")
class ReservationController(private val repo: ReservationRepo, private val eventRepo: EventRepo) {
    @GetMapping
    fun list(@RequestParam(required=false) eventId: Long?, @RequestParam(required=false) userEmail: String?): List<Reservation> = when {
        eventId != null -> repo.findByEventId(eventId)
        !userEmail.isNullOrBlank() -> repo.findByUserEmail(userEmail)
        else -> repo.findAll()
    }

    @PostMapping
    fun create(@RequestBody r: Reservation): Reservation = repo.save(r.copy(id=0))

    data class StatusReq(val status: String, val reason: String? = null)
    @PatchMapping("/{id}/status")
    fun changeStatus(@PathVariable id: Long, @RequestBody body: StatusReq): ResponseEntity<Reservation> = repo.findById(id).map { current ->
        val updated = current.copy(status= body.status, reason = body.reason)
        ResponseEntity.ok(repo.save(updated))
    }.orElse(ResponseEntity.notFound().build())
}

