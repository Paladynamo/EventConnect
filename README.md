r# 🎟️ EventConnect

**EventConnect** es una aplicación móvil desarrollada en **Kotlin** con **Jetpack Compose**, diseñada para conectar a tiendas, organizadores y amantes de los eventos en un solo lugar. Permite crear, descubrir y gestionar eventos de manera sencilla, moderna y visualmente atractiva.

---

## 🌟 Características principales

- 🔐 **Registro e inicio de sesión** con validación local mediante Room Database.
- 🏢 **Perfiles de usuario** para tiendas y usuarios comunes.
- 📅 **Creación, edición y eliminación de eventos**.
- 🖼️ **Soporte de imágenes** para eventos (galería y cámara).
- 🧠 **Gestión de datos locales** con `Room` y `Coroutines`.
- 🛠️ **Interfaz moderna** con Jetpack Compose.
- ☁️ Preparada para futura integración con servicios en la nube.

---

## 🖥️ Capturas de pantalla

| Pantalla principal | Registro | Crear evento |
|--------------------|-----------|---------------|
| ![HomeScreen](https://via.placeholder.com/250x520?text=Home) | ![Register](https://via.placeholder.com/250x520?text=Register) | ![CreateEvent](https://via.placeholder.com/250x520?text=Create+Event) |

*(Las imágenes son de ejemplo, reemplázalas con tus propias capturas del emulador o dispositivo).*

---

## ⚙️ Tecnologías utilizadas

| Componente | Descripción |
|-------------|-------------|
| 🛠️ **Kotlin** | Lenguaje principal del proyecto. |
| 🎨 **Jetpack Compose** | Construcción declarativa de la UI. |
| 📂 **Room Database** | Persistencia local de datos. |
| 🚀 **Coroutines** | Manejo asíncrono de operaciones. |
| 🔄 **Navigation Compose** | Flujo entre pantallas. |
| 🖼️ **Coil** | Carga de imágenes desde galería o cámara. |

---

## 🧭 Flujo principal de la app

1. **Inicio de sesión / Registro** → el usuario accede según su tipo (tienda o común).
2. **Pantalla principal (Home)** → visualiza los eventos recomendados.
3. **Creación de evento** (solo tiendas) → agrega título, categoría, fecha y descripción.
4. **Edición / Eliminación de evento** → gestión completa del contenido.

---

## 📋 Próximos pasos

- Integración con servicios en la nube para sincronización de datos.
- Mejora de la interfaz gráfica y experiencia de usuario.
- Implementación de notificaciones push para eventos destacados.

---

## 🛠️ Configuración del proyecto

1. Clona este repositorio:
   ```bash
   git clone https://github.com/tu-repositorio/EventConnect.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza las dependencias de Gradle.
4. Ejecuta la aplicación en un emulador o dispositivo físico.

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si deseas mejorar este proyecto, abre un issue o envía un pull request.

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo `LICENSE` para más detalles.

---

## Roadmap (próxima versión)

- Verificación de conectividad en el arranque y estrategia de sincronización offline-first con backend:
  - Detectar conectividad (Wi‑Fi/datos) y mostrar aviso si no hay Internet.
  - Encolar operaciones locales y sincronizarlas cuando exista conexión (WorkManager + Retrofit).
  - Mantener siempre la data en Room para modo sin conexión.

Estas funcionalidades se posponen para la siguiente iteración cuando el backend esté disponible.

---

✨ *EventConnect — Conecta, crea y vive experiencias únicas.* ✨
