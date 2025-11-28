package com.eventconnect.eventconnect.domain.model

import com.eventconnect.eventconnect.domain.enums.InterestType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_interests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_event_type",
            columnNames = ["user_id", "event_id", "type"]
        )
    ],
    indexes = [
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_event_id", columnList = "event_id")
    ]
)
data class UserInterest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Column(name = "user_email", nullable = false)
    var userEmail: String = "",

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: InterestType = InterestType.LIKE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = null,
        userId = 0,
        userEmail = "",
        eventId = 0,
        type = InterestType.LIKE,
        createdAt = LocalDateTime.now()
    )
}

