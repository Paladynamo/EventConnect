package com.example.eventconnect.web

import com.example.eventconnect.domain.User
import com.example.eventconnect.repo.UserRepo
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UsersController(private val userRepo: UserRepo) {
    private val encoder = BCryptPasswordEncoder()

    @GetMapping("/{email}")
    fun get(@PathVariable email:String): ResponseEntity<User> = userRepo.findByEmail(email)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    data class NameReq(val name:String)
    @PatchMapping("/{email}/name")
    fun updateName(@PathVariable email:String, @RequestBody body:NameReq): ResponseEntity<Map<String,Any>> {
        val user = userRepo.findByEmail(email) ?: return ResponseEntity.notFound().build()
        val saved = userRepo.save(user.copy(name = body.name))
        return ResponseEntity.ok(mapOf("name" to saved.name))
    }

    data class PassReq(val current:String, val new:String)
    @PatchMapping("/{email}/password")
    fun updatePassword(@PathVariable email:String, @RequestBody body:PassReq): ResponseEntity<Map<String,Any>> {
        val user = userRepo.findByEmail(email) ?: return ResponseEntity.notFound().build()
        if (!encoder.matches(body.current, user.passwordHash)) return ResponseEntity.status(403).body(mapOf("error" to "current invalid"))
        val saved = userRepo.save(user.copy(passwordHash = encoder.encode(body.new)))
        return ResponseEntity.ok(mapOf("updated" to true))
    }

    @DeleteMapping("/{email}")
    fun delete(@PathVariable email:String): ResponseEntity<Map<String,Any>> {
        val user = userRepo.findByEmail(email) ?: return ResponseEntity.notFound().build()
        userRepo.delete(user)
        return ResponseEntity.ok(mapOf("deleted" to true))
    }
}
