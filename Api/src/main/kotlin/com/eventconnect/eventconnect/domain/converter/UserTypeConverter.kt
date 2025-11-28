package com.eventconnect.eventconnect.domain.converter

import com.eventconnect.eventconnect.domain.enums.UserType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class UserTypeConverter : AttributeConverter<UserType, String> {
    
    override fun convertToDatabaseColumn(attribute: UserType?): String {
        return when (attribute) {
            UserType.USUARIO -> "Usuario"
            UserType.TIENDA -> "Tienda"
            null -> throw IllegalArgumentException("UserType no puede ser null")
        }
    }

    override fun convertToEntityAttribute(dbData: String?): UserType {
        return when (dbData?.trim()) {
            "Usuario" -> UserType.USUARIO
            "Tienda" -> UserType.TIENDA
            null -> throw IllegalArgumentException("UserType no puede ser null")
            else -> throw IllegalArgumentException("Valor de UserType desconocido: '$dbData'")
        }
    }
}

