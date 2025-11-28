package com.example.eventconnect.network

import android.util.Log
import com.example.eventconnect.data.Event
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object RemoteEventsApi {
    private const val BASE_URL = "http://10.0.2.2:8080/api"

    fun list(
        token: String?,
        store: String? = null,
        region: String? = null,
        comuna: String? = null,
        category: String? = null
    ): List<Event>? {
        return try {
            val params = mutableListOf<String>()
            if (!store.isNullOrBlank()) params += "store=" + URLEncoder.encode(store, "UTF-8")
            if (!region.isNullOrBlank()) params += "region=" + URLEncoder.encode(region, "UTF-8")
            if (!comuna.isNullOrBlank()) params += "comuna=" + URLEncoder.encode(comuna, "UTF-8")
            if (!category.isNullOrBlank()) params += "category=" + URLEncoder.encode(category, "UTF-8")
            val qs = if (params.isEmpty()) "" else ("?" + params.joinToString("&"))
            val url = URL("$BASE_URL/events$qs")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            conn.inputStream.bufferedReader().use { r ->
                val text = r.readText()
                val arr = JSONArray(text)
                val out = mutableListOf<Event>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    out += o.toEvent()
                }
                out
            }
        } catch (e: Exception) {
            Log.w("RemoteEventsApi", "list failed: ${e.message}")
            null
        }
    }
}

private fun JSONObject.optStringOrEmpty(name: String) = if (has(name) && !isNull(name)) getString(name) else ""

private fun JSONObject.optStringNullable(name: String): String? = if (has(name) && !isNull(name)) getString(name) else null

private fun JSONObject.optStringList(name: String): List<String>? {
    if (!has(name) || isNull(name)) return null
    val arr = getJSONArray(name)
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) list += arr.optString(i)
    return list
}

private fun JSONObject.optBool(name: String): Boolean {
    if (!has(name) || isNull(name)) return false
    return when (val v = get(name)) {
        is Boolean -> v
        is String -> v.equals("true", true) || v == "1"
        is Number -> v.toInt() != 0
        else -> false
    }
}

private fun JSONObject.toEvent(): Event {
    return Event(
        id = getInt("id"),
        title = getString("title"),
        description = optStringOrEmpty("description"),
        category = optStringOrEmpty("category"),
        date = optStringOrEmpty("date"),
        store = optStringOrEmpty("storeName"),
        region = optStringOrEmpty("region"),
        comuna = optStringOrEmpty("comuna"),
        addressStreet = optStringOrEmpty("addressStreet"),
        addressNumber = optStringOrEmpty("addressNumber"),
        imageUris = optStringList("imageUris"),
        hasCapacity = optBool("hasCapacity"),
        capacityTotal = optStringNullable("capacityTotal")?.toIntOrNull(),
        capacityAvailable = optStringNullable("capacityAvailable")?.toIntOrNull()
    )
}
