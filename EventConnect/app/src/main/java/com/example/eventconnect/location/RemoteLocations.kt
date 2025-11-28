package com.example.eventconnect.location

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiConfig {
    // Para emulador Android accediendo al host local
    const val BASE_URL: String = "http://10.0.2.2:8080"
}

// Devuelve Pair<regiones, comunasPorRegion> o null si falla
fun fetchRemoteLocations(): Pair<List<String>, Map<String, List<String>>>? {
    return try {
        val url = URL("${ApiConfig.BASE_URL}/locations")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2000
            readTimeout = 2000
        }
        conn.inputStream.bufferedReader().use { reader ->
            val text = reader.readText()
            val root = JSONObject(text)
            val regionsJson = root.optJSONArray("regions")
            val regions = buildList {
                if (regionsJson != null) for (i in 0 until regionsJson.length()) {
                    val r = regionsJson.optString(i)
                    if (!r.isNullOrBlank()) add(r)
                }
            }
            val mapJson = root.optJSONObject("comunasPorRegion")
            val comunasBy = mutableMapOf<String, List<String>>()
            if (mapJson != null) {
                for (key in mapJson.keys()) {
                    val arr = mapJson.optJSONArray(key)
                    val list = mutableListOf<String>()
                    if (arr != null) for (i in 0 until arr.length()) {
                        val c = arr.optString(i)
                        if (!c.isNullOrBlank()) list.add(c)
                    }
                    comunasBy[key] = list
                }
            }
            if (regions.isNotEmpty()) regions to comunasBy else null
        }
    } catch (e: Exception) {
        Log.w("RemoteLocations", "Failed to fetch /locations: ${e.message}")
        null
    }
}
