package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.domain.model.User
import com.eventconnect.eventconnect.dto.request.ChangePasswordRequest
import com.eventconnect.eventconnect.dto.request.UpdateProfileRequest
import com.eventconnect.eventconnect.dto.response.UserResponse
import com.eventconnect.eventconnect.exception.ResourceNotFoundException
import com.eventconnect.eventconnect.exception.UnauthorizedException
import com.eventconnect.eventconnect.mapper.UserMapper
import com.eventconnect.eventconnect.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun getProfile(userId: Long): UserResponse {
        val user = findUserById(userId)
        return UserMapper.toResponse(user)
    }

    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserResponse {
        val user = findUserById(userId)

        request.name?.let { user.name = it }
        request.storeName?.let { 
            if (user.userType == com.eventconnect.eventconnect.domain.enums.UserType.TIENDA) {
                user.storeName = it
            }
        }
        request.avatarUrl?.let { user.avatarUrl = it }

        val updatedUser = userRepository.save(user)
        return UserMapper.toResponse(updatedUser)
    }

    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = findUserById(userId)

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw UnauthorizedException("La contraseña actual es incorrecta")
        }

        val encodedPassword = passwordEncoder.encode(request.newPassword)
            ?: throw IllegalStateException("Error al codificar contraseña")
        user.password = encodedPassword
        userRepository.save(user)
    }

    fun deleteAccount(userId: Long) {
        val user = findUserById(userId)
        userRepository.delete(user)
    }

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado con ID: $userId") }
    }
}

