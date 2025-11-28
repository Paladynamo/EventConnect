package com.example.eventconnect.remote.service

import com.example.eventconnect.remote.dto.CreateReservationRequest
import com.example.eventconnect.remote.dto.RejectReservationRequest
import com.example.eventconnect.remote.dto.ReservationResponse
import retrofit2.http.*

interface ReservationService {
    @POST("reservations")
    suspend fun create(
        @Query("userId") userId: Long,
        @Body request: CreateReservationRequest
    ): ReservationResponse

    @GET("reservations/user")
    suspend fun getUserReservations(@Query("userId") userId: Long): List<ReservationResponse>

    @GET("reservations/event/{eventId}")
    suspend fun getEventReservations(
        @Path("eventId") eventId: Long,
        @Query("userId") userId: Long
    ): List<ReservationResponse>

    @PUT("reservations/{id}/approve")
    suspend fun approve(
        @Path("id") id: Long,
        @Query("userId") userId: Long
    ): ReservationResponse

    @PUT("reservations/{id}/reject")
    suspend fun reject(
        @Path("id") id: Long,
        @Query("userId") userId: Long,
        @Body request: RejectReservationRequest
    ): ReservationResponse
}
