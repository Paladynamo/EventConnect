package com.eventconnect.eventconnect.domain.model

import com.eventconnect.eventconnect.domain.enums.OperationStatus
import com.eventconnect.eventconnect.domain.enums.OperationType
import jakarta.persistence.*
import org.hibernate.type.SqlTypes
import org.hibernate.annotations.JdbcTypeCode
import java.time.LocalDateTime

@Entity
@Table(
    name = "pending_operations",
    indexes = [
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_status", columnList = "status")
    ]
)
data class PendingOperation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: OperationType = OperationType.EVENT_CREATE,

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var payload: String = "{}", // JSON string

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OperationStatus = OperationStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at", nullable = true)
    var processedAt: LocalDateTime? = null
) {
    constructor() : this(
        id = null,
        userId = 0,
        type = OperationType.EVENT_CREATE,
        payload = "{}",
        status = OperationStatus.PENDING,
        retryCount = 0,
        createdAt = LocalDateTime.now(),
        processedAt = null
    )
}

