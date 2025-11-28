package com.example.eventconnect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    // ✅ Inserta un usuario nuevo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // ✅ Verifica credenciales de inicio de sesión
    // Login: email case-insensitive, password exacta
    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) AND password = :password LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): User?

    // ✅ Verifica si el correo ya está registrado
    // Duplicados: evitar por case
    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    // ✅ Actualiza la contraseña de un usuario por email
    // Update password por email (case-insensitive)
    @Query("UPDATE users SET password = :newPassword WHERE lower(email) = lower(:email)")
    suspend fun updatePassword(email: String, newPassword: String): Int

    // ✅ Obtiene un usuario específico por ID
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    // ✅ Devuelve todos los usuarios (solo para pruebas o debug)
    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsers(): List<User>

    // Actualiza el nombre de un usuario por email
    @Query("UPDATE users SET name = :newName WHERE lower(email) = lower(:email)")
    suspend fun updateName(email: String, newName: String): Int

    // Actualiza la contraseña de un usuario verificando la contraseña actual
    @Query("UPDATE users SET password = :newPassword WHERE lower(email) = lower(:email) AND password = :currentPassword")
    suspend fun updatePasswordChecked(email: String, currentPassword: String, newPassword: String): Int

    // Elimina una cuenta de usuario por email
    @Query("DELETE FROM users WHERE lower(email) = lower(:email)")
    suspend fun deleteByEmail(email: String): Int
}
