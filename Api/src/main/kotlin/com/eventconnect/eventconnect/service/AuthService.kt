package com.eventconnect.eventconnect.service

import com.eventconnect.eventconnect.domain.enums.UserType
import com.eventconnect.eventconnect.domain.model.User
import com.eventconnect.eventconnect.dto.request.LoginRequest
import com.eventconnect.eventconnect.dto.request.RegisterRequest
import com.eventconnect.eventconnect.dto.response.AuthResponse
import com.eventconnect.eventconnect.dto.response.UserResponse
import com.eventconnect.eventconnect.exception.BadRequestException
import com.eventconnect.eventconnect.exception.UnauthorizedException
import com.eventconnect.eventconnect.mapper.UserMapper
import com.eventconnect.eventconnect.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(request: RegisterRequest): AuthResponse {
        // Validar que si es TIENDA, tenga storeName
        if (request.userType == UserType.TIENDA && request.storeName.isNullOrBlank()) {
            throw BadRequestException("El nombre de la tienda es requerido para usuarios tipo Tienda")
        }

        // Verificar si el email ya existe
        if (userRepository.existsByEmailIgnoreCase(request.email)) {
            throw BadRequestException("El email ya está registrado")
        }

        // Crear nuevo usuario
        val encodedPassword = passwordEncoder.encode(request.password) 
            ?: throw IllegalStateException("Error al codificar contraseña")
        val user = User(
            name = request.name,
            email = request.email.lowercase().trim(),
            password = encodedPassword,
            userType = request.userType,
            storeName = request.storeName
        )

        val savedUser = userRepository.save(user)

        // TODO: Generar JWT token (por ahora retornamos un token temporal)
        val token = "temp_token_${savedUser.id}"

        return AuthResponse(
            token = token,
            user = UserMapper.toResponse(savedUser)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmailIgnoreCase(request.email.lowercase())
            .orElseThrow { UnauthorizedException("Credenciales inválidas") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw UnauthorizedException("Credenciales inválidas")
        }

        // TODO: Generar JWT token (por ahora retornamos un token temporal)
        val token = "temp_token_${user.id}"

        return AuthResponse(
            token = token,
            user = UserMapper.toResponse(user)
        )
    }

    fun forgotPassword(email: String) {
        // TODO: Implementar lógica de recuperación de contraseña
        // Por ahora solo validamos que el usuario exista
        if (!userRepository.existsByEmailIgnoreCase(email.lowercase())) {
            throw BadRequestException("No se encontró un usuario con ese email")
        }
        // En producción, aquí se enviaría un email con un link de recuperación
    }
}

