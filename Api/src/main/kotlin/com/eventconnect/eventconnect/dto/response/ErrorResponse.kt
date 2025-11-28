package com.eventconnect.eventconnect.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null
)

