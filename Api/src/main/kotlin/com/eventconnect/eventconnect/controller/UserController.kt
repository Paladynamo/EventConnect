package com.eventconnect.eventconnect.controller

import com.eventconnect.eventconnect.dto.request.ChangePasswordRequest
import com.eventconnect.eventconnect.dto.request.UpdateProfileRequest
import com.eventconnect.eventconnect.dto.response.MessageResponse
import com.eventconnect.eventconnect.dto.response.UserResponse
import com.eventconnect.eventconnect.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/profile")
    fun getProfile(@RequestParam userId: Long): ResponseEntity<UserResponse> {
        val profile = userService.getProfile(userId)
        return ResponseEntity.ok(profile)
    }

    @PutMapping("/profile")
    fun updateProfile(
        @RequestParam userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val updatedProfile = userService.updateProfile(userId, request)
        return ResponseEntity.ok(updatedProfile)
    }

    @PutMapping("/password")
    fun changePassword(
        @RequestParam userId: Long,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<MessageResponse> {
        userService.changePassword(userId, request)
        return ResponseEntity.ok(MessageResponse("Contraseña actualizada exitosamente"))
    }

    @DeleteMapping("/account")
    fun deleteAccount(@RequestParam userId: Long): ResponseEntity<MessageResponse> {
        userService.deleteAccount(userId)
        return ResponseEntity.ok(MessageResponse("Cuenta eliminada exitosamente"))
    }
}

