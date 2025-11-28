package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.dto.response.NotificationResponse
import com.eventconnect.eventconnect.mapper.NotificationMapper
import com.eventconnect.eventconnect.repository.EventUpdateNoticeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationService(
    private val notificationRepository: EventUpdateNoticeRepository
) {

    fun getUserNotifications(userId: Long): List<NotificationResponse> {
        val notifications = notificationRepository.findByUserId(userId)
        return notifications.map { NotificationMapper.toResponse(it) }
    }

    fun markNotificationsAsRead(userId: Long) {
        val unreadNotifications = notificationRepository.findByUserIdAndRead(userId, false)
        unreadNotifications.forEach { it.read = true }
        notificationRepository.saveAll(unreadNotifications)
    }

    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByUserIdAndRead(userId, false)
    }
}

