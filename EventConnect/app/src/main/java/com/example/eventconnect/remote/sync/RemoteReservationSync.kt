package com.example.eventconnect.remote.sync

import android.content.Context
import com.example.eventconnect.data.AppDatabase
import com.example.eventconnect.data.PendingOperation
import com.example.eventconnect.remote.RetrofitProvider
import com.example.eventconnect.remote.dto.RejectReservationRequest
import com.example.eventconnect.remote.service.ReservationService
import com.example.eventconnect.util.SessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteReservationSync(private val context: Context) {
    private val service by lazy { RetrofitProvider.retrofit(context).create(ReservationService::class.java) }
    private val pendingDao = AppDatabase.getDatabase(context).pendingOperationDao()

    suspend fun changeStatus(id: Int, status: String, reason: String? = null) {
        withContext(Dispatchers.IO) {
            val userId = SessionHelper.getUserId(context)
            if (userId == null) {
                // Si no hay userId, guardar como operación pendiente
                val payload = buildString {
                    append("{\"id\":").append(id)
                    append(",\"status\":\"").append(status).append("\"")
                    append(",\"reason\":")
                    append(if (reason == null) "null" else "\"" + reason.replace("\"", "\\\"") + "\"")
                    append("}")
                }
                pendingDao.insert(PendingOperation(type = "RESERVATION_STATUS", payload = payload))
                return@withContext
            }
            
            try {
                when (status.uppercase()) {
                    "APPROVED" -> {
                        service.approve(id.toLong(), userId)
                    }
                    "REJECTED" -> {
                        service.reject(
                            id.toLong(), 
                            userId, 
                            RejectReservationRequest(reason ?: "Reserva rechazada")
                        )
                    }
                    else -> {
                        // Estado no soportado, guardar como pendiente
                        val payload = buildString {
                            append("{\"id\":").append(id)
                            append(",\"status\":\"").append(status).append("\"")
                            append(",\"reason\":")
                            append(if (reason == null) "null" else "\"" + reason.replace("\"", "\\\"") + "\"")
                            append("}")
                        }
                        pendingDao.insert(PendingOperation(type = "RESERVATION_STATUS", payload = payload))
                    }
                }
            } catch (ex: Exception) {
                val payload = buildString {
                    append("{\"id\":").append(id)
                    append(",\"status\":\"").append(status).append("\"")
                    append(",\"reason\":")
                    append(if (reason == null) "null" else "\"" + reason.replace("\"", "\\\"") + "\"")
                    append("}")
                }
                pendingDao.insert(PendingOperation(type = "RESERVATION_STATUS", payload = payload))
            }
        }
    }
}
