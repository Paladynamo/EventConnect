package com.example.eventconnect.web

import com.example.eventconnect.domain.Event
import com.example.eventconnect.repo.EventRepo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/events")
class EventController(private val repo: EventRepo) {
    @GetMapping
    fun list(@RequestParam(required=false) store: String?, @RequestParam(required=false) region: String?, @RequestParam(required=false) comuna: String?, @RequestParam(required=false) category: String?): List<Event> {
        return when {
            !store.isNullOrBlank() -> repo.findByStoreName(store)
            !region.isNullOrBlank() && !comuna.isNullOrBlank() && !category.isNullOrBlank() -> repo.findByRegionAndComunaAndCategory(region, comuna, category)
            !region.isNullOrBlank() && !comuna.isNullOrBlank() -> repo.findByRegionAndComuna(region, comuna)
            !region.isNullOrBlank() && !category.isNullOrBlank() -> repo.findByRegionAndCategory(region, category)
            !region.isNullOrBlank() -> repo.findByRegion(region)
            else -> repo.findAll()
        }
    }

    @PostMapping
    fun create(@RequestBody e: Event): Event = repo.save(e.copy(id=0))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody incoming: Event): ResponseEntity<Event> = repo.findById(id).map {
        val updated = it.copy(
            title = incoming.title,
            description = incoming.description,
            category = incoming.category,
            date = incoming.date,
            region = incoming.region,
            comuna = incoming.comuna,
            addressStreet = incoming.addressStreet,
            addressNumber = incoming.addressNumber,
            hasCapacity = incoming.hasCapacity,
            capacityTotal = incoming.capacityTotal,
            capacityAvailable = incoming.capacityAvailable,
            imageUris = incoming.imageUris
        )
        ResponseEntity.ok(repo.save(updated))
    }.orElse(ResponseEntity.notFound().build())

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = repo.deleteById(id)
}

