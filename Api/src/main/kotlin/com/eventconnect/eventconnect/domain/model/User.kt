package com.eventconnect.eventconnect.domain.model

import com.eventconnect.eventconnect.domain.converter.UserTypeConverter
import com.eventconnect.eventconnect.domain.enums.UserType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_email", columnList = "email", unique = true)
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Convert(converter = UserTypeConverter::class)
    @Column(nullable = false, name = "user_type")
    var userType: UserType = UserType.USUARIO,

    @Column(name = "store_name", nullable = true)
    var storeName: String? = null,

    @Column(name = "avatar_url", nullable = true)
    var avatarUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = null,
        name = "",
        email = "",
        password = "",
        userType = UserType.USUARIO,
        storeName = null,
        avatarUrl = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

