package com.example.eventconnect.data

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        val json = JSONArray()
        list.forEach { json.put(it) }
        return json.toString()
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { idx -> arr.getString(idx) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

