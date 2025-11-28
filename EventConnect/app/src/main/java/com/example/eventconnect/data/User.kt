//**esta sera la clase del user, falta añadirla, german esta en fase de contruccion */
package com.example.eventconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // Nombre del usuario o tienda
    val email: String,          // Correo electrónico único
    val password: String,       // Contraseña
    val userType: String,       // "Usuario" o "Tienda"
    val storeName: String? = "" // Opcional para tiendas/productores
)
