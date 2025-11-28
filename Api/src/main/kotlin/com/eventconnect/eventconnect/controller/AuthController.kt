package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.dto.request.ForgotPasswordRequest
import com.eventconnect.eventconnect.dto.request.LoginRequest
import com.eventconnect.eventconnect.dto.request.RegisterRequest
import com.eventconnect.eventconnect.dto.response.AuthResponse
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<MessageResponse> {
        authService.forgotPassword(request.email)
        return ResponseEntity.ok(MessageResponse("Si el email existe, se enviará un enlace de recuperación"))
    }
}

