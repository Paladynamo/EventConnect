package com.example.eventconnect.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper para obtener información de la sesión del usuario
 */
object SessionHelper {
    private const val PREFS_NAME = "user_session"
    
    fun getUserId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getLong("userId", -1L)
        return if (userId != -1L) userId else null
    }
    
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("token", null)
    }
    
    fun getUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("email", null)
    }
    
    fun getUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("name", null)
    }
    
    fun getUserType(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("type", null)
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null && getUserId(context) != null
    }
}

