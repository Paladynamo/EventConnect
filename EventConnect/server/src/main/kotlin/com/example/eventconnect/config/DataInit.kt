package com.example.eventconnect.config

import com.example.eventconnect.domain.Event
import com.example.eventconnect.domain.User
import com.example.eventconnect.repo.EventRepo
import com.example.eventconnect.repo.UserRepo
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
class DataInit {
    @Bean
    fun runner(userRepo: UserRepo, eventRepo: EventRepo) = CommandLineRunner {
        val encoder = BCryptPasswordEncoder()
        if (userRepo.findByEmail("store@example.com") == null) {
            userRepo.save(User(name="Tienda Demo", email="store@example.com", passwordHash=encoder.encode("demo123"), userType="STORE", storeName="La Tienda"))
        }
        if (eventRepo.count() == 0L) {
            eventRepo.save(Event(title="Torneo TCG", storeName="La Tienda", category="TCG", date="10/12/2025", region="Metropolitana", comuna="Santiago", hasCapacity=true, capacityTotal=20, capacityAvailable=20))
            eventRepo.save(Event(title="Juegos de Mesa Nocturno", storeName="La Tienda", category="Juegos de Mesa", date="15/12/2025", region="Metropolitana", comuna="Providencia", hasCapacity=false))
        }
    }
}

