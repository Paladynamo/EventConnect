package com.eventconnect.eventconnect.repository

import com.eventconnect.eventconnect.domain.enums.OperationStatus
import com.eventconnect.eventconnect.domain.model.PendingOperation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingOperationRepository : JpaRepository<PendingOperation, Long> {
    fun findByUserId(userId: Long): List<PendingOperation>
    fun findByStatus(status: OperationStatus): List<PendingOperation>
    fun findByUserIdAndStatus(userId: Long, status: OperationStatus): List<PendingOperation>
}

