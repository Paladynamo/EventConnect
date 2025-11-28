package com.example.eventconnect.sync

import android.content.Context
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.dto.RejectReservationRequest
import com.example.eventconnect.remote.service.EventService
import com.example.eventconnect.remote.service.ReservationService
import com.example.eventconnect.remote.service.ReviewService
import com.example.eventconnect.remote.service.UserReviewDto
import com.example.eventconnect.util.SessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SyncProcessor(private val context: Context) {
    private val retrofit = RetrofitProvider.retrofit(context)
    private val eventService = retrofit.create(EventService::class.java)
    private val reservationService = retrofit.create(ReservationService::class.java)
    private val reviewService = retrofit.create(ReviewService::class.java)

    suspend fun process(ops: List<PendingOperation>): List<PendingOperation> = withContext(Dispatchers.IO) {
        val remaining = mutableListOf<PendingOperation>()
        val userId = SessionHelper.getUserId(context)
        
        if (userId == null) {
            // Si no hay userId, no podemos sincronizar
            return@withContext ops
        }
        
        for (op in ops) {
            val keep = try { !handle(op, userId) } catch (_: Exception) { true }
            if (keep) remaining += op
        }
        remaining
    }

    private suspend fun handle(op: PendingOperation, userId: Long): Boolean {
        return when (op.type) {
            "EVENT_CREATE" -> true // Evento ya local; podría reenviar si backend requiere, omitimos para demo
            "RESERVATION_APPROVE" -> {
                val id = JSONObject(op.payload).getInt("reservationId")
                reservationService.approve(id.toLong(), userId)
                true
            }
            "RESERVATION_CANCEL" -> {
                // CANCELLED no está soportado directamente, guardar como pendiente
                true
            }
            "RESERVATION_REJECT" -> {
                val o = JSONObject(op.payload)
                val id = o.getInt("reservationId")
                val reason = o.optString("reason", "Reserva rechazada")
                reservationService.reject(id.toLong(), userId, RejectReservationRequest(reason))
                true
            }
            "RESERVATION_STATUS" -> {
                val o = JSONObject(op.payload)
                val id = o.getInt("id")
                val status = o.getString("status")
                val reason = o.optString("reason", null)
                
                when (status.uppercase()) {
                    "APPROVED" -> {
                        reservationService.approve(id.toLong(), userId)
                        true
                    }
                    "REJECTED" -> {
                        reservationService.reject(
                            id.toLong(), 
                            userId, 
                            RejectReservationRequest(reason ?: "Reserva rechazada")
                        )
                        true
                    }
                    else -> {
                        // Estado no soportado, conservar
                        true
                    }
                }
            }
            "USER_REVIEW_CREATE" -> {
                val o = JSONObject(op.payload)
                val email = o.getString("email")
                val reviewer = o.getString("reviewerStore")
                val rating = o.getInt("rating")
                val comment = if (o.isNull("comment")) null else o.getString("comment")
                reviewService.create(UserReviewDto(reviewedUserEmail=email, reviewerStore=reviewer, rating=rating, comment=comment))
                true
            }
            else -> false // desconocido -> conservar
        }
    }
}
