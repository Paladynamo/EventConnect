package com.example.eventconnect.web

import com.example.eventconnect.domain.User
import com.example.eventconnect.repo.UserRepo
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/auth")
class AuthController(private val userRepo: UserRepo, @Value("\${jwt.secret}") private val secret: String) {
    private val encoder = BCryptPasswordEncoder()

    data class RegisterReq(val name: String, val email: String, val password: String, val userType: String, val storeName: String? = null)
    data class LoginReq(val email: String, val password: String)
    data class TokenResp(val token: String, val email: String, val name: String, val userType: String, val storeName: String?)

    @PostMapping("/register")
    fun register(@RequestBody body: RegisterReq): ResponseEntity<Any> {
        if (userRepo.findByEmail(body.email) != null) return ResponseEntity.badRequest().body(mapOf("error" to "Email ya existe"))
        val user = userRepo.save(User(name = body.name, email = body.email, passwordHash = encoder.encode(body.password), userType = body.userType, storeName = body.storeName))
        return ResponseEntity.ok(mapOf("id" to user.id))
    }

    @PostMapping("/login")
    fun login(@RequestBody body: LoginReq): ResponseEntity<Any> {
        val user = userRepo.findByEmail(body.email) ?: return ResponseEntity.status(401).body(mapOf("error" to "Credenciales"))
        if (!encoder.matches(body.password, user.passwordHash)) return ResponseEntity.status(401).body(mapOf("error" to "Credenciales"))
        val token = Jwts.builder()
            .setSubject(user.email)
            .claim("name", user.name)
            .claim("type", user.userType)
            .claim("store", user.storeName)
            .setExpiration(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 12))
            .signWith(SignatureAlgorithm.HS256, secret.toByteArray())
            .compact()
        return ResponseEntity.ok(TokenResp(token, user.email, user.name, user.userType, user.storeName))
    }
}

