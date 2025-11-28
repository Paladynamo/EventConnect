package com.example.eventconnect.remote.service

import retrofit2.http.GET
import retrofit2.http.Path

// Consumo Nager.Date feriados públicos
// Ej: https://date.nager.at/api/v3/PublicHolidays/2025/CL

data class HolidayDto(val date: String, val localName: String, val name: String, val countryCode: String)

interface HolidayService {
    @GET("PublicHolidays/{year}/{country}")
    suspend fun list(@Path("year") year: Int, @Path("country") country: String): List<HolidayDto>
}

