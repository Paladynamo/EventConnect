package com.example.eventconnect

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.eventconnect.data.User
import com.example.eventconnect.data.UserDao
import com.example.eventconnect.data.Event
import com.example.eventconnect.data.EventDao

// 🔹 Subimos la versión a 2 porque agregamos un nuevo campo a la entidad Event
@Database(entities = [User::class, Event::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eventconnect_db"
                )
                    // ⚠️ IMPORTANTE: Destruye la DB vieja si hay incompatibilidad de esquema
                    // Esto evita crashes si cambian las entidades (solo para desarrollo)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
