package com.example.eventconnect.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/locations")
class LocationsController {
    private val regiones = listOf(
        "Metropolitana" to listOf("Santiago", "Providencia", "Las Condes"),
        "Valparaiso" to listOf("Valparaiso", "Viña del Mar", "Quilpue"),
        "Biobio" to listOf("Concepcion", "Talcahuano")
    )

    @GetMapping("/regions")
    fun regions(): List<String> = regiones.map { it.first }

    @GetMapping("/comunas")
    fun comunas(@RequestParam region: String): List<String> =
        regiones.firstOrNull { it.first.equals(region, ignoreCase = true) }?.second ?: emptyList()
}
