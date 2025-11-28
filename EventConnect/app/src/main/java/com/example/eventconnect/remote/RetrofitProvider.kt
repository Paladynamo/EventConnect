package com.example.eventconnect.remote

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitProvider {
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    // Crear instancia de Moshi con el adaptador de Kotlin
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory()) // Crucial para deserializar data classes de Kotlin
        .build()

    private fun client(context: Context): OkHttpClient {
        // Cambiar a BODY para ver la respuesta completa en Logcat
        val logging = HttpLoggingInterceptor().apply { 
            level = HttpLoggingInterceptor.Level.BODY 
        }
        val auth = Interceptor { chain ->
            val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val req = if (token != null) chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build() else chain.request()
            chain.proceed(req)
        }
        return OkHttpClient.Builder().addInterceptor(auth).addInterceptor(logging).build()
    }

    fun retrofit(context: Context): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi)) // Usar la instancia de Moshi configurada
        .client(client(context))
        .build()
}

