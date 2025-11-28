package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.dto.request.SyncOperationsRequest
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.service.SyncService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncService: SyncService
) {

    @PostMapping("/operations")
    fun syncOperations(
        @RequestParam userId: Long,
        @Valid @RequestBody request: SyncOperationsRequest
    ): ResponseEntity<MessageResponse> {
        val response = syncService.processSyncOperations(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}

