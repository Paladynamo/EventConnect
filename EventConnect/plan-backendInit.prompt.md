Objetivo: Iniciar backend de EventConnect (MVP) con Ktor para servir datos de usuarios, eventos y reservas, preparando futura sincronización con la app Android.

Alcance inicial (MVP 0):
1. Infraestructura
   - Nuevo módulo Gradle :server (Kotlin JVM + Application)
   - Dependencias Ktor (core, netty, serialization, content-negotiation, call-logging, status-pages, cors)
   - Logging con logback
   - Serialización JSON (kotlinx.serialization)
   - Configuración básica (puerto 8080, entorno dev)
2. Modelo de dominio (simplificado, espejo parcial de Room entities móviles)
   - User(id, name, email, passwordHash, userType, storeName?)
   - Event(id, title, description, category, date, storeName, location{region, comuna, addressStreet, addressNumber}, imageUris[], hasCapacity, capacityTotal, capacityAvailable)
   - Reservation(id, eventId, userId, status, reason?, createdAt)
3. Repositorios en memoria (List + atomic counters) para bootstrap
   - Thread-safe (ConcurrentHashMap / synchronized)
   - Interfaz Repository y simple implementación InMemory
4. Endpoints REST iniciales
   - GET /health -> {status:"ok"}
   - Users:
     * POST /users (crear) -> devuelve user
     * GET /users/{id}
     * POST /auth/login (email,password) -> token simulado
   - Events:
     * GET /events (filtros opcionales: store, region, comuna, category)
     * GET /events/{id}
     * POST /events (crear)
     * PATCH /events/{id} (actualizar capacidad / campos básicos)
   - Reservations:
     * POST /events/{id}/reservations (crear solicitud)
     * GET /events/{id}/reservations (lista para organizador)
     * GET /reservations?userId= (lista usuario)
     * POST /reservations/{id}/status (body: status, reason?)
5. Seguridad (MVP)
   - Token simple (UUID) almacenado en memoria asociado a userId
   - Header Authorization: Bearer <token>
   - Middleware que resuelve currentUser (except health, auth y creación de usuario)
6. Errores y respuestas
   - StatusPages para mapear excepciones: NotFound, BadRequest, Unauthorized
   - Validación simple de entrada (campos obligatorios)
7. Preparar puntos de extensión futura
   - Reemplazo de repos in-memory por persistencia (PostgreSQL + Exposed / JPA / Mongo)
   - Autenticación real (JWT + refresh tokens)
   - Sincronización offline: endpoints para batch de PendingOperation
   - WebSockets para avisos de updates de eventos
8. Scripts / Ejecución
   - Gradle task :server:run (puerto 8080)
   - README breve con instrucciones de arranque
9. Próximos pasos (no implementados en MVP 0)
   - Upload de imágenes (prever /files y storage S3 / local)
   - Paginación y cache headers
   - Tests (Ktor server tests + integración)

Orden de implementación
A. Añadir módulo :server y ajustes settings.gradle.kts
B. build.gradle.kts con plugins y dependencias
C. Crear modelos @Serializable
D. Crear repositorios in-memory
E. Crear capa de auth (AuthService + TokenStore)
F. Routing: health, users, auth, events, reservations
G. Configurar Application.kt con instalaciones de Ktor
H. Configuración resources (application.conf, logback.xml)
I. Compilar y probar local (:server:run)
J. Actualizar README (resumen backend + ejecución)

Edge Cases a cubrir en código
- Filtro eventos sin resultados -> []
- Reservar en evento sin capacidad -> 409 Conflict
- Cambio de estado de reserva ya finalizada -> 400
- Rechazo con motivo vacío -> default "Sin motivo"
- Login inválido -> 401
- Acceso a evento inexistente -> 404
- Crear usuario con email repetido -> 409

Contrato básico de ejemplos
POST /users {
  "name":"Ana","email":"ana@mail.com","password":"123","userType":"Usuario"
}
-> 201 {user}
POST /auth/login {"email":"ana@mail.com","password":"123"}
-> 200 {"token":"...","userId":1}
Authorization: Bearer <token>
POST /events {...}
-> 201 {event}
POST /events/1/reservations {}
-> 201 {reservation}

Fin del plan MVP 0 backend.

