package com.eventconnect.eventconnect.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@Order(1)
class DatabaseConnectionTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(DatabaseConnectionTest::class.java)

    @PostConstruct
    fun testConnection() {
        try {
            logger.info("🔍 Probando conexión a la base de datos...")
            
            // Consulta simple para verificar la conexión
            val result = jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            
            if (result == 1) {
                logger.info("✅ Conexión a la base de datos exitosa!")
                
                // Intentar verificar que las tablas existen
                try {
                    val tableCount = jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*) 
                        FROM information_schema.tables 
                        WHERE table_schema = 'public'
                        """.trimIndent(),
                        Int::class.java
                    )
                    logger.info("📊 Tablas encontradas en la base de datos: $tableCount")
                } catch (e: Exception) {
                    logger.warn("⚠️ No se pudo contar las tablas: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Error al conectar a la base de datos: ${e.message}")
            logger.error("Detalles del error:", e)
            throw RuntimeException("No se pudo conectar a la base de datos", e)
        }
    }
}


