package com.example.eventconnect.web

import com.example.eventconnect.domain.UserReview
import com.example.eventconnect.repo.UserReviewRepo
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/reviews")
class ReviewController(private val repo: UserReviewRepo) {
    @GetMapping
    fun list(@RequestParam(required=false) email: String?): List<UserReview> = if (!email.isNullOrBlank()) repo.findByReviewedUserEmail(email) else repo.findAll()

    @PostMapping
    fun create(@RequestBody r: UserReview): UserReview = repo.save(r.copy(id=0))

    @GetMapping("/stats")
    fun stats(@RequestParam email: String): Map<String, Any> {
        val reviews = repo.findByReviewedUserEmail(email)
        val avg = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
        return mapOf("count" to reviews.size, "average" to avg)
    }
}

