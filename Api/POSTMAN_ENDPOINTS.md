# Endpoints de EventConnect - Postman

**Base URL:** `http://localhost:8080`

---

## 🔐 Autenticación (`/api/auth`)

### 1. Registrar Usuario
**POST** `/api/auth/register`

**Body (JSON):**
```json
{
  "name": "Juan Pérez",
  "email": "juan@example.com",
  "password": "password123",
  "userType": "USUARIO",
  "storeName": null
}
```

**Ejemplo para TIENDA:**
```json
{
  "name": "Tienda de Juegos",
  "email": "tienda@example.com",
  "password": "password123",
  "userType": "TIENDA",
  "storeName": "Mi Tienda de Juegos"
}
```

**Valores de `userType`:**
- `USUARIO`
- `TIENDA`

---

### 2. Iniciar Sesión
**POST** `/api/auth/login`

**Body (JSON):**
```json
{
  "email": "juan@example.com",
  "password": "password123"
}
```

---

### 3. Recuperar Contraseña
**POST** `/api/auth/forgot-password`

**Body (JSON):**
```json
{
  "email": "juan@example.com"
}
```

---

## 📅 Eventos (`/api/events`)

### 1. Obtener Todos los Eventos (con filtros opcionales)
**GET** `/api/events`

**Query Parameters (todos opcionales):**
- `region`: String (ej: "Región Metropolitana")
- `comuna`: String (ej: "Santiago")
- `category`: EventCategory (ej: "TCG", "JUEGOS_DE_MESA", "EVENTOS")
- `page`: Int (default: 0)
- `size`: Int (default: 20)

**Ejemplos:**
- `/api/events`
- `/api/events?region=Región Metropolitana&comuna=Santiago`
- `/api/events?category=TCG&page=0&size=10`

**Valores de `category`:**
- `TCG`
- `JUEGOS_DE_MESA`
- `EVENTOS`

---

### 2. Obtener Evento por ID
**GET** `/api/events/{id}`

**Ejemplo:**
- `/api/events/1`

---

### 3. Crear Evento
**POST** `/api/events?userId=1`
A la hora de crear eventos debes tener en consideracion mandar el id de la tienda si no no dejara si usuario normal

**Body (JSON):**
```json
{
  "title": "Torneo de Magic: The Gathering",
  "description": "Torneo competitivo de MTG con premios",
  "category": "TCG",
  "date": "2024-12-25T18:00:00",
  "region": "Región Metropolitana",
  "comuna": "Santiago",
  "addressStreet": "Av. Providencia",
  "addressNumber": "1234",
  "imageUrls": [
    "https://example.com/image1.jpg",
    "https://example.com/image2.jpg"
  ],
  "hasCapacity": true,
  "capacityTotal": 50
}
```

**Ejemplo sin capacidad:**
```json
{
  "title": "Noche de Juegos de Mesa",
  "description": "Ven a jugar juegos de mesa con nosotros",
  "category": "JUEGOS_DE_MESA",
  "date": "2024-12-20T19:00:00",
  "region": "Valparaíso",
  "comuna": "Viña del Mar",
  "addressStreet": "Calle Principal",
  "addressNumber": "567",
  "imageUrls": [],
  "hasCapacity": false,
  "capacityTotal": null
}
```

**Formato de fecha:** `YYYY-MM-DDTHH:mm:ss` (ISO 8601)

---

### 4. Actualizar Evento
**PUT** `/api/events/{id}?userId=1`

**Body (JSON):** (todos los campos son opcionales)
```json
{
  "title": "Torneo de Magic Actualizado",
  "description": "Nueva descripción del evento",
  "category": "TCG",
  "date": "2024-12-26T18:00:00",
  "region": "Región Metropolitana",
  "comuna": "Las Condes",
  "addressStreet": "Av. Nueva",
  "addressNumber": "5678",
  "imageUrls": ["https://example.com/new-image.jpg"],
  "hasCapacity": true,
  "capacityTotal": 60,
  "capacityAvailable": 45
}
```

**Ejemplo mínimo (solo actualizar título):**
```json
{
  "title": "Nuevo Título del Evento"
}
```

---

### 5. Eliminar Evento
**DELETE** `/api/events/{id}?userId=1`

**Ejemplo:**
- `/api/events/1?userId=1`

---

### 6. Obtener Eventos por Tienda
**GET** `/api/events/store/{storeId}`

**Ejemplo:**
- `/api/events/store/1`

---

## 🎫 Reservas (`/api/reservations`)

### 1. Crear Reserva
**POST** `/api/reservations?userId=1`

**Body (JSON):**
```json
{
  "eventId": 1
}
```

---

### 2. Obtener Reservas del Usuario
**GET** `/api/reservations/user?userId=1`

**Ejemplo:**
- `/api/reservations/user?userId=1`

---

### 3. Obtener Reservas de un Evento
**GET** `/api/reservations/event/{eventId}?userId=1`

**Ejemplo:**
- `/api/reservations/event/1?userId=1`

---

### 4. Aprobar Reserva
**PUT** `/api/reservations/{id}/approve?userId=1`

**Ejemplo:**
- `/api/reservations/1/approve?userId=1`

---

### 5. Rechazar Reserva
**PUT** `/api/reservations/{id}/reject?userId=1`

**Body (JSON):**
```json
{
  "reason": "No hay más cupos disponibles"
}
```

---

## 👤 Usuarios (`/api/users`)

### 1. Obtener Perfil
**GET** `/api/users/profile?userId=1`

**Ejemplo:**
- `/api/users/profile?userId=1`

---

### 2. Actualizar Perfil
**PUT** `/api/users/profile?userId=1`

**Body (JSON):** (todos los campos son opcionales)
```json
{
  "name": "Juan Pérez Actualizado",
  "storeName": "Nuevo Nombre de Tienda",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

**Ejemplo mínimo:**
```json
{
  "name": "Nuevo Nombre"
}
```

---

### 3. Cambiar Contraseña
**PUT** `/api/users/password?userId=1`

**Body (JSON):**
```json
{
  "currentPassword": "password123",
  "newPassword": "newpassword456"
}
```

---

### 4. Eliminar Cuenta
**DELETE** `/api/users/account?userId=1`

**Ejemplo:**
- `/api/users/account?userId=1`

---

## 🔔 Notificaciones (`/api/notifications`)

### 1. Obtener Notificaciones
**GET** `/api/notifications?userId=1`

**Ejemplo:**
- `/api/notifications?userId=1`

---

### 2. Marcar Notificaciones como Leídas
**PUT** `/api/notifications/mark-read?userId=1`

**Ejemplo:**
- `/api/notifications/mark-read?userId=1`

---

## 🔄 Sincronización (`/api/sync`)

### 1. Sincronizar Operaciones
**POST** `/api/sync/operations?userId=1`

**Body (JSON):**
```json
{
  "operations": [
    {
      "type": "EVENT_CREATE",
      "payload": "{\"title\":\"Evento Offline\",\"description\":\"...\"}"
    },
    {
      "type": "RESERVATION_UPDATE",
      "payload": "{\"reservationId\":1,\"status\":\"APPROVED\"}"
    }
  ]
}
```

**Valores de `type`:**
- `EVENT_CREATE`
- `EVENT_UPDATE`
- `RESERVATION_CREATE`
- `RESERVATION_UPDATE`
- `RESERVATION_DELETE`

**Ejemplo con múltiples operaciones:**
```json
{
  "operations": [
    {
      "type": "EVENT_CREATE",
      "payload": "{\"title\":\"Torneo Local\",\"description\":\"Torneo realizado offline\",\"category\":\"TCG\",\"date\":\"2024-12-20T18:00:00\",\"region\":\"Metropolitana\",\"comuna\":\"Santiago\",\"addressStreet\":\"Calle\",\"addressNumber\":\"123\"}"
    },
    {
      "type": "RESERVATION_CREATE",
      "payload": "{\"eventId\":5}"
    }
  ]
}
```

---

## 📝 Notas Importantes

1. **Puerto:** Por defecto Spring Boot usa el puerto `8080`. Si cambias el puerto, actualiza la base URL.

2. **Content-Type:** Todos los endpoints POST/PUT requieren el header:
   ```
   Content-Type: application/json
   ```

3. **Parámetros de Query:** Los endpoints que requieren `userId` lo reciben como query parameter (`?userId=1`).

4. **Fechas:** El formato de fecha debe ser ISO 8601: `YYYY-MM-DDTHH:mm:ss` (ej: `2024-12-25T18:00:00`).

5. **Validaciones:**
   - Email debe ser válido
   - Contraseña mínimo 6 caracteres
   - Nombre entre 2 y 100 caracteres
   - Título de evento máximo 200 caracteres

6. **Enums:**
   - `UserType`: `USUARIO`, `TIENDA`
   - `EventCategory`: `TCG`, `JUEGOS_DE_MESA`, `EVENTOS`
   - `OperationType`: `EVENT_CREATE`, `EVENT_UPDATE`, `RESERVATION_CREATE`, `RESERVATION_UPDATE`, `RESERVATION_DELETE`

