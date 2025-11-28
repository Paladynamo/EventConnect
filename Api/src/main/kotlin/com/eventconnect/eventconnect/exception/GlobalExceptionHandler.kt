package com.eventconnect.eventconnect.exception

import com.eventconnect.eventconnect.dto.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Recurso no encontrado", path = request.getDescription(false)))
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Solicitud inválida", path = request.getDescription(false)))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = ex.message ?: "No autorizado", path = request.getDescription(false)))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.joinToString(", ") { error ->
            when (error) {
                is FieldError -> "${error.field}: ${error.defaultMessage}"
                else -> error.defaultMessage ?: "Error de validación"
            }
        }
        logger.warn("Validation error: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "Errores de validación: $errors", path = request.getDescription(false)))
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: org.springframework.http.converter.HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.warn("JSON parse error: ${ex.message}")
        val errorMessage = when {
            ex.message?.contains("non-null") == true -> {
                "Campos requeridos faltantes en el JSON. Verifica que todos los campos obligatorios estén presentes."
            }
            ex.message?.contains("Cannot construct instance") == true -> {
                "Error al parsear el JSON. Verifica que el formato sea correcto y que todos los campos requeridos estén presentes."
            }
            else -> "Error al leer el JSON: ${ex.message}"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = errorMessage, path = request.getDescription(false)))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error: ${ex.message}", ex)
        val errorMessage = if (ex.message != null) {
            "Error interno del servidor: ${ex.message}"
        } else {
            "Error interno del servidor"
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = errorMessage, path = request.getDescription(false)))
    }
}

