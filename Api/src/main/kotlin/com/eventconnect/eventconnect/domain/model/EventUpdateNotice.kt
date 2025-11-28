package com.eventconnect.eventconnect.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_update_notices",
    indexes = [
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_user_read", columnList = "user_id,is_read")
    ]
)
data class EventUpdateNotice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "user_email", nullable = false)
    var userEmail: String = "",

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0,

    @Column(name = "event_title", nullable = false)
    var eventTitle: String = "",

    @Column(name = "store_name", nullable = false)
    var store: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var changes: String = "",

    @Column(name = "is_read", nullable = false)
    var read: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = null,
        userId = 0,
        userEmail = "",
        eventId = 0,
        eventTitle = "",
        store = "",
        changes = "",
        read = false,
        createdAt = LocalDateTime.now()
    )
}

