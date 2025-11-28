package com.example.eventconnect.domain

import jakarta.persistence.*

@Entity
@Table(name = "events")
data class Event(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable=false) val title: String,
    @Column(columnDefinition="TEXT") val description: String = "",
    val category: String = "",
    val date: String = "",
    @Column(nullable=false) val storeName: String,
    val region: String = "",
    val comuna: String = "",
    val addressStreet: String = "",
    val addressNumber: String = "",
    val hasCapacity: Boolean = false,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="event_images", joinColumns=[JoinColumn(name="event_id")])
    @Column(name="uri")
    val imageUris: List<String> = emptyList()
)

