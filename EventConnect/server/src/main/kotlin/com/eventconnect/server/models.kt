package com.eventconnect.server

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Int,
    val title: String,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val storeName: String = "",
    val region: String = "",
    val comuna: String = "",
    val addressStreet: String = "",
    val addressNumber: String = "",
    val imageUris: List<String> = emptyList(),
    val hasCapacity: Boolean = false,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null
)

class NotFoundException(msg: String): RuntimeException(msg)
class ConflictException(msg: String): RuntimeException(msg)
class UnauthorizedException(msg: String): RuntimeException(msg)
class BadRequestException(msg: String): RuntimeException(msg)

class EventRepository {
    private val events = mutableListOf<Event>(
        Event(1, "Friday Night Magic", "Torneo semanal", "TCG", "10/12/2025", "ManaBase", "Metropolitana de Santiago", "Ñuñoa", "Av. Irarrázaval", "1234", hasCapacity = true, capacityTotal = 32, capacityAvailable = 12),
        Event(2, "Board Games Night", "Juegos de mesa para todos", "Juegos de Mesa", "15/12/2025", "Meeple House", "Valparaíso", "Viña del Mar", "Calle Uno", "555"),
        Event(3, "Torneo Modern", "Formato Modern", "TCG", "05/01/2026", "ManaBase", "Metropolitana de Santiago", "Providencia", "Av. Providencia", "2001", hasCapacity = true, capacityTotal = 64, capacityAvailable = 40)
    )
    private var nextId = 4

    fun list(store: String?, region: String?, comuna: String?, category: String?): List<Event> {
        return events.asSequence()
            .filter { store.isNullOrBlank() || it.storeName.equals(store, true) }
            .filter { region.isNullOrBlank() || it.region.equals(region, true) }
            .filter { comuna.isNullOrBlank() || it.comuna.equals(comuna, true) }
            .filter { category.isNullOrBlank() || it.category.equals(category, true) }
            .sortedByDescending { it.id }
            .toList()
    }

    fun get(id: Int): Event = events.firstOrNull { it.id == id } ?: throw NotFoundException("Evento no encontrado")

    fun create(req: CreateEventRequest, ownerStore: String): Event {
        val e = Event(
            id = nextId++,
            title = req.title,
            description = req.description ?: "",
            category = req.category ?: "",
            date = req.date,
            storeName = ownerStore,
            region = req.region ?: "",
            comuna = req.comuna ?: "",
            addressStreet = req.addressStreet ?: "",
            addressNumber = req.addressNumber ?: "",
            imageUris = req.imageUris ?: emptyList(),
            hasCapacity = req.hasCapacity ?: false,
            capacityTotal = req.capacityTotal,
            capacityAvailable = req.capacityAvailable
        )
        events += e
        return e
    }

    fun update(id: Int, req: UpdateEventRequest): Event {
        val cur = get(id)
        val up = cur.copy(
            title = req.title ?: cur.title,
            description = req.description ?: cur.description,
            category = req.category ?: cur.category,
            date = req.date ?: cur.date,
            region = req.region ?: cur.region,
            comuna = req.comuna ?: cur.comuna,
            addressStreet = req.addressStreet ?: cur.addressStreet,
            addressNumber = req.addressNumber ?: cur.addressNumber,
            imageUris = req.imageUris ?: cur.imageUris,
            hasCapacity = req.hasCapacity ?: cur.hasCapacity,
            capacityTotal = req.capacityTotal ?: cur.capacityTotal,
            capacityAvailable = req.capacityAvailable ?: cur.capacityAvailable
        )
        val idx = events.indexOfFirst { it.id == id }
        events[idx] = up
        return up
    }
}

@Serializable
data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val date: String,
    val region: String? = null,
    val comuna: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val imageUris: List<String>? = null,
    val hasCapacity: Boolean? = null,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null
)

@Serializable
data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val date: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val addressStreet: String? = null,
    val addressNumber: String? = null,
    val imageUris: List<String>? = null,
    val hasCapacity: Boolean? = null,
    val capacityTotal: Int? = null,
    val capacityAvailable: Int? = null
)

// Placeholders mínimos para compilar, si no existen aún en tu backend
@Serializable data class User(val id: Int = 1, val name: String = "User", val userType: String = "Tienda", val storeName: String? = null)
class UserRepository { fun getByEmail(email: String) = User(1, "TiendaEjemplo", "Tienda", "ManaBase") }
class AuthService(private val ur: UserRepository) { fun login(email: String, pass: String) = mapOf("token" to "demo"); fun resolve(token: String) = ur.getByEmail("demo@x.com") }

@Serializable data class Reservation(val id: Int = 1, val eventId: Int = 1, val userId: Int = 1, val status: String = "PENDING", val reason: String? = null)
class ReservationRepository(private val evRepo: EventRepository) {
    private val reservations = mutableListOf<Reservation>()
    private var nextId = 1
    fun create(eventId: Int, userId: Int): Reservation { val r = Reservation(nextId++, eventId, userId); reservations += r; return r }
    fun listForEvent(eventId: Int) = reservations.filter { it.eventId == eventId }
    fun listForUser(userId: Int) = reservations.filter { it.userId == userId }
    fun get(id: Int) = reservations.first { it.id == id }
    fun updateStatus(id: Int, status: String, reason: String?) = get(id).copy(status = status, reason = reason).also { r ->
        val idx = reservations.indexOfFirst { it.id == id }; reservations[idx] = r
    }
}

// Reseñas / calificaciones de usuarios
@Serializable
data class UserReview(
    val id: Int,
    val reviewedUserEmail: String,
    val reviewerStore: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class ReviewRepository {
    private val reviews = mutableListOf<UserReview>()
    private var nextId = 1

    fun listForUser(email: String): List<UserReview> = reviews.filter { it.reviewedUserEmail.equals(email, true) }.sortedByDescending { it.createdAt }
    fun statsForUser(email: String): Pair<Double, Int> {
        val list = reviews.filter { it.reviewedUserEmail.equals(email, true) }
        val count = list.size
        val avg = if (count > 0) list.map { it.rating }.average() else 0.0
        return avg to count
    }
    fun create(email: String, reviewerStore: String, rating: Int, comment: String?): UserReview {
        require(rating in 1..5) { "rating inválido" }
        val r = UserReview(id = nextId++, reviewedUserEmail = email, reviewerStore = reviewerStore, rating = rating, comment = comment)
        reviews += r
        return r
    }
}

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class CreateUserRequest(val name: String, val email: String, val password: String, val userType: String, val storeName: String? = null)
@Serializable data class CreateReservationRequest(val userId: Int)
@Serializable data class UpdateReservationStatusRequest(val status: String, val reason: String? = null)
@Serializable data class ReviewCreateRequest(val rating: Int, val comment: String? = null)
