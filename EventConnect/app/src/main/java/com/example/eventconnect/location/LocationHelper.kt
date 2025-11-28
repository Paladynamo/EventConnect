package com.example.eventconnect.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(context: Context) {
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        return try {
            fused.lastLocation.await()
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationHighAccuracy(timeoutMs: Long = 5000L): Location? {
        return try {
            val cts = CancellationTokenSource()
            val loc = withTimeoutOrNull(timeoutMs) {
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }
            loc
        } catch (_: Exception) { null }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOrLast(timeoutMs: Long = 5000L): Location? {
        val current = getCurrentLocationHighAccuracy(timeoutMs)
        if (current != null) return current
        return getLastLocation()
    }

    fun isLocationEnabled(context: Context): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            gps || net
        } catch (_: Exception) { false }
    }
}
