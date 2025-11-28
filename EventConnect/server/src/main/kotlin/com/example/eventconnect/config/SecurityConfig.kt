package com.example.eventconnect.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.beans.factory.annotation.Autowired

@Configuration
class SecurityConfig {
    @Autowired lateinit var jwtFilter: JwtFilter

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**", "/h2-console/**", "/locations/**").permitAll()
                it.requestMatchers("/events", "/events/*").permitAll() // permitir lectura pública
                it.anyRequest().authenticated()
            }
        http.headers { it.frameOptions { fo -> fo.disable() } }
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
