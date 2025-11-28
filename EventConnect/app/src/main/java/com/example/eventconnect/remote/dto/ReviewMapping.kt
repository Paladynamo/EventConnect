package com.example.eventconnect.remote.dto

import com.example.eventconnect.data.UserReview
import com.example.eventconnect.remote.service.UserReviewDto

fun UserReviewDto.toDomain(): UserReview = UserReview(
    id = (id?:0).toInt(),
    reviewedUserEmail = reviewedUserEmail,
    reviewerStore = reviewerStore,
    rating = rating,
    comment = comment,
    createdAt = createdAt ?: System.currentTimeMillis()
)
