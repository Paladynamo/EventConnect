package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.dto.response.NotificationResponse
import com.eventconnect.eventconnect.service.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(@RequestParam userId: Long): ResponseEntity<List<NotificationResponse>> {
        val notifications = notificationService.getUserNotifications(userId)
        return ResponseEntity.ok(notifications)
    }

    @PutMapping("/mark-read")
    fun markNotificationsAsRead(@RequestParam userId: Long): ResponseEntity<MessageResponse> {
        notificationService.markNotificationsAsRead(userId)
        return ResponseEntity.ok(MessageResponse("Notificaciones marcadas como leídas"))
    }
}

