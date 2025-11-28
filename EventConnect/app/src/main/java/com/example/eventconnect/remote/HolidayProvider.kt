package com.example.eventconnect.remote

import android.content.Context
import com.example.eventconnect.remote.service.HolidayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate

object HolidayProvider {
    private const val BASE = "https://date.nager.at/api/v3/"
    private fun retrofit(): Retrofit = Retrofit.Builder().baseUrl(BASE).addConverterFactory(MoshiConverterFactory.create()).build()
    private val service: HolidayService = retrofit().create(HolidayService::class.java)

    private var cache: List<com.example.eventconnect.remote.service.HolidayDto>? = null
    private var cacheYear: Int? = null

    suspend fun upcomingChile(context: Context): List<com.example.eventconnect.remote.service.HolidayDto> = withContext(Dispatchers.IO) {
        val year = LocalDate.now().year
        if (cache != null && cacheYear == year) return@withContext cache!!.filter { LocalDate.parse(it.date).isAfter(LocalDate.now().minusDays(1)) }.take(5)
        val list = try { service.list(year, "CL") } catch (_: Exception) { emptyList() }
        cache = list; cacheYear = year
        list.filter { LocalDate.parse(it.date).isAfter(LocalDate.now().minusDays(1)) }.take(5)
    }
}

