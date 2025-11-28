package com.eventconnect.eventconnect.domain.model

import com.eventconnect.eventconnect.domain.enums.EventCategory
import jakarta.persistence.*
import org.hibernate.type.SqlTypes
import org.hibernate.annotations.JdbcTypeCode
import java.time.LocalDateTime

@Entity
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_store_id", columnList = "store_id"),
        Index(name = "idx_region_comuna", columnList = "region,comuna"),
        Index(name = "idx_category", columnList = "category")
    ]
)
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: EventCategory = EventCategory.EVENTOS,

    @Column(nullable = false)
    var date: LocalDateTime = LocalDateTime.now(),

    @Column(name = "store_id", nullable = false)
    var storeId: Long = 0,

    @Column(name = "store_name", nullable = false)
    var storeName: String = "",

    @Column(nullable = false)
    var region: String = "",

    @Column(nullable = false)
    var comuna: String = "",

    @Column(name = "address_street", nullable = false)
    var addressStreet: String = "",

    @Column(name = "address_number", nullable = false)
    var addressNumber: String = "",

    @Column(name = "image_urls", nullable = true, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = StringListConverter::class)
    var imageUrls: List<String> = emptyList(),

    @Column(name = "has_capacity", nullable = false)
    var hasCapacity: Boolean = false,

    @Column(name = "capacity_total", nullable = true)
    var capacityTotal: Int? = null,

    @Column(name = "capacity_available", nullable = true)
    var capacityAvailable: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = null,
        title = "",
        description = "",
        category = EventCategory.EVENTOS,
        date = LocalDateTime.now(),
        storeId = 0,
        storeName = "",
        region = "",
        comuna = "",
        addressStreet = "",
        addressNumber = "",
        imageUrls = emptyList(),
        hasCapacity = false,
        capacityTotal = null,
        capacityAvailable = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Converter
class StringListConverter : jakarta.persistence.AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        if (attribute == null || attribute.isEmpty()) return "[]"
        // Convertir lista a JSON array string para JSONB
        return "[\"${attribute.joinToString("\",\"")}\"]"
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData == null || dbData.isBlank() || dbData == "[]") return emptyList()
        // Parsear JSON array string desde JSONB
        // Remover corchetes y comillas, luego split por comas
        val cleaned = dbData.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .trim()
        if (cleaned.isEmpty()) return emptyList()
        return cleaned.split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}

