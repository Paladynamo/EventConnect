package com.example.eventconnect.remote.service

import com.example.eventconnect.remote.dto.CreateEventRequest
import com.example.eventconnect.remote.dto.EventResponse
import com.example.eventconnect.remote.dto.MessageResponse
import com.example.eventconnect.remote.dto.PageResponse
import com.example.eventconnect.remote.dto.UpdateEventRequest
import retrofit2.http.*

interface EventService {
    @GET("events")
    suspend fun list(
        @Query("region") region: String? = null,
        @Query("comuna") comuna: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PageResponse<EventResponse>

    @GET("events/{id}")
    suspend fun getById(@Path("id") id: Long): EventResponse

    @POST("events")
    suspend fun create(
        @Query("userId") userId: Long,
        @Body request: CreateEventRequest
    ): EventResponse

    @PUT("events/{id}")
    suspend fun update(
        @Path("id") id: Long,
        @Query("userId") userId: Long,
        @Body request: UpdateEventRequest
    ): EventResponse

    @DELETE("events/{id}")
    suspend fun delete(
        @Path("id") id: Long,
        @Query("userId") userId: Long
    ): MessageResponse

    @GET("events/store/{storeId}")
    suspend fun getByStore(@Path("storeId") storeId: Long): List<EventResponse>
}

