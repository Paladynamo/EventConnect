package com.example.eventconnect.location

import android.content.Context

/**
 * Stub de utilidades de Google Places/Maps.
 * Si no hay SDK configurado, devolvemos null para que el caller haga fallback al Geocoder.
 * Cuando integres Places SDK, reemplaza el cuerpo de validateAddress con la consulta real.
 */
object PlacesUtils {
    /**
     * Valida una dirección en línea. Retorna:
     *  - true si se valida explícitamente con el proveedor,
     *  - false si se determinó que es inválida,
     *  - null si no hay integración/resultado (para que el caller haga fallback o permita continuar).
     */
    fun validateAddress(context: Context, addressLine: String): Boolean? {
        // Sin integración real: devolver null para que el flujo use Geocoder o continúe.
        return null
    }
}

