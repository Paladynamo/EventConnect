package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.domain.enums.OperationStatus
import com.eventconnect.eventconnect.domain.enums.OperationType
import com.eventconnect.eventconnect.domain.model.PendingOperation
import com.eventconnect.eventconnect.dto.request.SyncOperationsRequest
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.repository.PendingOperationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SyncService(
    private val pendingOperationRepository: PendingOperationRepository
) {

    fun processSyncOperations(userId: Long, request: SyncOperationsRequest): MessageResponse {
        val operations = request.operations.map { syncOp ->
            PendingOperation(
                userId = userId,
                type = syncOp.type,
                payload = syncOp.payload,
                status = OperationStatus.PENDING
            )
        }

        pendingOperationRepository.saveAll(operations)

        return MessageResponse("${operations.size} operaciones pendientes registradas para sincronización")
    }

    fun getPendingOperations(userId: Long): List<PendingOperation> {
        return pendingOperationRepository.findByUserIdAndStatus(userId, OperationStatus.PENDING)
    }
}

