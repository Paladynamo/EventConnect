package com.example.eventconnect.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

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
}
