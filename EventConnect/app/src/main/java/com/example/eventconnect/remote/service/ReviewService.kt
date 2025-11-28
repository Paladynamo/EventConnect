package com.example.eventconnect.remote.service

import retrofit2.http.*

data class UserReviewDto(
    val id: Long? = null,
    val reviewedUserEmail: String,
    val reviewerStore: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Long? = null
)

interface ReviewService {
    @GET("reviews")
    suspend fun list(@Query("email") email: String? = null): List<UserReviewDto>

    @POST("reviews")
    suspend fun create(@Body dto: UserReviewDto): UserReviewDto

    @GET("reviews/stats")
    suspend fun stats(@Query("email") email: String): Map<String,Any>
}

