package com.eventconnect.eventconnect.dto.request

import com.eventconnect.eventconnect.domain.enums.OperationType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class SyncOperationsRequest(
    @field:NotEmpty(message = "Debe haber al menos una operación")
    @field:Valid
    val operations: List<SyncOperation>
)

data class SyncOperation(
    @field:NotNull(message = "El tipo de operación es requerido")
    val type: OperationType,

    @field:NotBlank(message = "El payload es requerido")
    val payload: String
)

