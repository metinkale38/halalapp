package dev.metinkale.halalapp.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

private val schemaPattern = Regex("[A-Za-z_][A-Za-z0-9_]*")

object DatabaseFactory {
    fun init(poolSize: Int? = null) {
        val schema = System.getenv("DB_SCHEMA")?.trim()?.takeIf { it.isNotEmpty() }
        if (schema != null) {
            require(schemaPattern.matches(schema)) { "Ungültiges DB_SCHEMA: $schema" }
        }

        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_URL")
            username = System.getenv("JDBC_USERNAME")
            password = System.getenv("JDBC_PASSWORD")
            maximumPoolSize = poolSize ?: System.getenv("DB_POOL_SIZE")?.toInt() ?: 10
            minimumIdle = 3
            keepaliveTime = 60000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            if (schema != null) {
                connectionInitSql = "SET search_path TO \"$schema\", public"
            }

            validate()
        }
        Database.connect(HikariDataSource(config))

        DatabaseMigrator.migrate()
    }
}
