package com.eventconnect.server

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json

fun main() { embeddedServer(Netty, port = 8080) { module() }.start(wait = true) }

fun Application.module() {
    val userRepo = UserRepository()
    val eventRepo = EventRepository()
    val reservationRepo = ReservationRepository(eventRepo)
    val authService = AuthService(userRepo)

    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; prettyPrint = true }) }
    install(CallLogging)
    install(CORS) { anyHost(); allowHeader(HttpHeaders.Authorization); allowHeader(HttpHeaders.ContentType) }
    install(StatusPages) {
        exception<NotFoundException> { call, cause -> call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message)) }
        exception<ConflictException> { call, cause -> call.respond(HttpStatusCode.Conflict, mapOf("error" to cause.message)) }
        exception<UnauthorizedException> { call, cause -> call.respond(HttpStatusCode.Unauthorized, mapOf("error" to cause.message)) }
        exception<BadRequestException> { call, cause -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message)) }
        exception<Throwable> { call, cause -> cause.printStackTrace(); call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "error"))) }
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
        // Derivar regiones/comunas desde eventos existentes
        get("/locations") {
            val all = eventRepo.list(null, null, null, null)
            val byRegion = all.groupBy { it.region }.filterKeys { !it.isNullOrBlank() }
            val regions = byRegion.keys.sorted()
            val comunasPorRegion = byRegion.mapValues { (_, evs) -> evs.map { it.comuna }.filter { it.isNotBlank() }.distinct().sorted() }
            call.respond(mapOf("regions" to regions, "comunasPorRegion" to comunasPorRegion))
        }

        post("/users") {
            val body = call.receive<CreateUserRequest>()
            if (body.name.isBlank() || body.email.isBlank() || body.password.isBlank() || body.userType.isBlank()) throw BadRequestException("Campos obligatorios vacíos")
            call.respond(HttpStatusCode.Created, userRepo.create(body))
        }
        post("/auth/login") { val body = call.receive<LoginRequest>(); call.respond(authService.login(body.email, body.password)) }
        fun PipelineContext<Unit, ApplicationCall>.currentUser(): User {
            val header = call.request.header(HttpHeaders.Authorization) ?: throw UnauthorizedException("Falta Authorization")
            val token = header.removePrefix("Bearer ").trim()
            return authService.resolve(token) ?: throw UnauthorizedException("Token inválido")
        }
        route("/events") {
            get {
                currentUser()
                val qp = call.request.queryParameters
                call.respond(eventRepo.list(qp["store"], qp["region"], qp["comuna"], qp["category"]))
            }
            post {
                val user = currentUser(); if (user.userType != "Tienda") throw UnauthorizedException("Solo tiendas pueden crear eventos")
                val body = call.receive<CreateEventRequest>()
                call.respond(HttpStatusCode.Created, eventRepo.create(body, user.storeName ?: user.name))
            }
            get("/{id}") { currentUser(); val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("id inválido"); call.respond(eventRepo.get(id)) }
            patch("/{id}") {
                val user = currentUser(); val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("id inválido")
                val body = call.receive<UpdateEventRequest>(); val ev = eventRepo.get(id)
                if (!ev.storeName.equals(user.storeName ?: user.name, true)) throw UnauthorizedException("No eres dueño del evento")
                call.respond(eventRepo.update(id, body))
            }
            post("/{id}/reservations") {
                val user = currentUser(); val eventId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("id inválido")
                val req = call.receive<CreateReservationRequest>(); if (req.userId != user.id) throw BadRequestException("userId no coincide")
                call.respond(HttpStatusCode.Created, reservationRepo.create(eventId, user.id))
            }
            get("/{id}/reservations") {
                val user = currentUser(); val eventId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("id inválido")
                val event = eventRepo.get(eventId)
                if (!event.storeName.equals(user.storeName ?: user.name, true)) throw UnauthorizedException("No eres dueño del evento")
                call.respond(reservationRepo.listForEvent(eventId))
            }
        }
        get("/reservations") { val user = currentUser(); call.respond(reservationRepo.listForUser(user.id)) }
        post("/reservations/{id}/status") {
            val user = currentUser(); val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("id inválido")
            val body = call.receive<UpdateReservationStatusRequest>(); val reservation = reservationRepo.get(id); val event = eventRepo.get(reservation.eventId)
            if (body.status == "CANCELLED" && reservation.userId == user.id) {
                call.respond(reservationRepo.updateStatus(id, "CANCELLED", body.reason))
            } else {
                if (!event.storeName.equals(user.storeName ?: user.name, true)) throw UnauthorizedException("No autorizado")
                if (body.status !in listOf("APPROVED", "REJECTED")) throw BadRequestException("Estado inválido")
                call.respond(reservationRepo.updateStatus(id, body.status, body.reason))
            }
        }
    }
}

