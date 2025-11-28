package com.example.eventconnect.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(@Value("\${jwt.secret}") private val secret: String): OncePerRequestFilter() {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val path = request.requestURI
        if (path.startsWith("/auth") || path.startsWith("/h2") || path.startsWith("/locations") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response); return
        }
        val auth = request.getHeader("Authorization")
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.status = 401; response.writer.write("Missing token"); return
        }
        val token = auth.substringAfter("Bearer ").trim()
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
        } catch (ex: Exception) {
            response.status = 401; response.writer.write("Invalid token"); return
        }
        filterChain.doFilter(request, response)
    }
}
